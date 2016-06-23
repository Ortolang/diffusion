package fr.ortolang.diffusion.runtime.engine.task;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.Status;

import org.activiti.engine.delegate.DelegateExecution;

import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagFile;
import gov.loc.repository.bagit.Manifest.Algorithm;
import gov.loc.repository.bagit.utilities.SimpleResult;

public class LoadBagContentTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(LoadBagContentTask.class.getName());

    public static final String NAME = "Load Bag Content";
    public static final String DATA_PREFIX = "data/";
    public static final String HEAD_PREFIX = "data/head/";
    public static final String SNAPSHOTS_PREFIX = "data/snapshots/";
    public static final String OBJECTS_PREFIX = "objects/";
    public static final String METADATA_PREFIX = "metadata/";

    public LoadBagContentTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                LOGGER.log(Level.FINE, "committing active user transaction.");
                getUserTransaction().commit();
            } else {
                LOGGER.log(Level.FINE, "user transaction in state : " + getUserTransaction().getStatus());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e);
        }
        checkParameters(execution);
        Path bagpath = Paths.get(execution.getVariable(BAG_PATH_PARAM_NAME, String.class));
        Bag bag = loadBag(bagpath);
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Bag file loaded from local file: " + bag.getFile()));

        LOGGER.log(Level.FINE, "- list versions");
        List<String> versions = searchVersions(bag);

        LOGGER.log(Level.FINE, "- list snapshots to publish");
        List<String> snapshots = searchSnapshotsToPublish(bag);

        LOGGER.log(Level.FINE, "- search tag names for snapshots");
        Map<String, String> tagnames = searchTagsForSnapshots(bag, snapshots);

        LOGGER.log(Level.FINE, "- build import script");
        StringBuilder builder = new StringBuilder();
        appendWorkspaceInformations(builder, bag);
        String pversion = null;
        Set<String> pobjects = Collections.emptySet();
        Set<String> pmetadata = Collections.emptySet();
        for (String version : versions) {
            Set<String> objects = listObjects(version, bag.getPayload());
            appendObjectsOperations(builder, bag, pversion, version, pobjects, objects);
            Set<String> metadata = listMetadata(version, bag.getPayload());
            appendMetadataOperations(builder, bag, pversion, version, pmetadata, metadata);
            if (!version.equals(Workspace.HEAD)) {
                builder.append("snapshot-workspace\t").append("\r\n");
            }
            pversion = version;
            pobjects = objects;
            pmetadata = metadata;
        }
        try {
            bag.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "- error during closing bag", e);
        }
        try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "starting new user transaction.");
                getUserTransaction().begin();
            } else {
                LOGGER.log(Level.FINE, "user transaction in state : " + getUserTransaction().getStatus());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }
        LOGGER.log(Level.INFO, "- import script generated.");
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), "Import script generated: \r\n" + builder.toString(), null));
        throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import script generated"));
        execution.setVariable(BAG_VERSIONS_PARAM_NAME, versions);
        execution.setVariable(IMPORT_OPERATIONS_PARAM_NAME, builder.toString());
        execution.setVariable(SNAPSHOTS_TO_PUBLISH_PARAM_NAME, snapshots);
        execution.setVariable(SNAPSHOTS_TAGS_PARAM_NAME, tagnames);
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (!execution.hasVariable(BAG_PATH_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + BAG_PATH_PARAM_NAME + " is not set");
        }
    }

    private Bag loadBag(Path bagpath) throws RuntimeEngineTaskException {
        if (!Files.exists(bagpath)) {
            throw new RuntimeEngineTaskException("bag file " + bagpath + " does not exists");
        }
        BagFactory factory = new BagFactory();
        Bag bag = factory.createBag(bagpath.toFile());
        SimpleResult result = bag.verifyPayloadManifests();
        if (!result.isSuccess()) {
            LOGGER.log(Level.WARNING, "bag verification failed: " + result.messagesToString());
            throw new RuntimeEngineTaskException("bag verification failed: " + result.messagesToString());
        }
        return bag;
    }

    private List<String> searchVersions(Bag bag) {
        Collection<BagFile> payload = bag.getPayload();
        Set<Integer> snapshots = new HashSet<>();
        boolean headExists = false;
        for (BagFile bagfile : payload) {
            if (bagfile.getFilepath().startsWith(SNAPSHOTS_PREFIX)) {
                String[] parts = bagfile.getFilepath().substring(SNAPSHOTS_PREFIX.length()).split("/");
                if (parts.length <= 1 || parts[0].length() <= 0 || parts[1].length() <= 0) {
                    LOGGER.log(Level.INFO, "Unparsable snapshot hierarchy found: " + Arrays.deepToString(parts));
                }
                Integer index;
                try {
                    index = Integer.decode(parts[0]);
                    if ( snapshots.add(index) ) {
                        LOGGER.log(Level.INFO, "Found new version with index: " + index);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "Snapshot index is not a number: " + parts[0]);
                }
            }
            if (!headExists && bagfile.getFilepath().startsWith(HEAD_PREFIX)) {
                LOGGER.log(Level.INFO, "Found head");
                headExists = true;
            }
        }

        List<String> versions = new ArrayList<String>();
        for (Integer snapshot : snapshots) {
            versions.add("snapshots/" + snapshot);
        }
        if (headExists) {
            versions.add(Workspace.HEAD);
        }
        return versions;
    }

    private List<String> searchSnapshotsToPublish(Bag bag) throws RuntimeEngineTaskException {
        BagFile propFile = bag.getBagFile("data/publication.properties");
        if (propFile == null || !propFile.exists()) {
            return Collections.emptyList();
        }
        try {
            Properties props = new Properties();
            props.load(propFile.newInputStream());
            return Arrays.asList(props.getProperty("publish").split(","));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to list snapshots to publish", e);
            throw new RuntimeEngineTaskException("unable to list snapshots to publish", e);
        }
    }

    private Map<String, String> searchTagsForSnapshots(Bag bag, List<String> snapshots) throws RuntimeEngineTaskException {
        BagFile propFile = bag.getBagFile("data/publication.properties");
        if (propFile == null || !propFile.exists()) {
            return Collections.emptyMap();
        }
        try {
            Properties props = new Properties();
            props.load(propFile.newInputStream());
            if (!props.containsKey("tags")) {
                return Collections.emptyMap();
            }
            List<String> tags = Arrays.asList(props.getProperty("tags").split(","));
            Map<String, String> tagnames = new HashMap<String, String> ();
            for ( String tag : tags ) {
                String snapshot = tag.substring(0, tag.indexOf(":"));
                String tagname = tag.substring(tag.indexOf(":") + 1);
                if ( snapshots.contains(snapshot) ) {
                    tagnames.put(snapshot, tagname);
                }
            }
            return tagnames;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to find tags names for snapshots", e);
            throw new RuntimeEngineTaskException("unable to find tags names for snapshots", e);
        }
    }

    private void appendWorkspaceInformations(StringBuilder builder, Bag bag) throws RuntimeEngineTaskException {
        BagFile propFile = bag.getBagFile("data/workspace.properties");
        if (propFile == null || !propFile.exists()) {
            throw new RuntimeEngineTaskException("Workspace properties file does not exists, create one !!");
        }
        try {
            Properties props = new Properties();
            props.load(propFile.newInputStream());
            builder.append("create-workspace\t").append(props.getProperty("alias"))
                    .append("\t").append(props.getProperty("name"))
                    .append("\t").append(props.getProperty("type"))
                    .append("\t").append(props.getProperty("owner") != null ? props.getProperty("owner") : "")
                    .append("\t").append(props.getProperty("members") != null ? props.getProperty("members") : "").append("\r\n");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to append workspace informations", e);
            throw new RuntimeEngineTaskException("unable to append workspace informations", e);
        }
    }

    private Set<String> listObjects(String version, Collection<BagFile> payload) {
        Set<String> objects = new HashSet<String>();
        String prefix = DATA_PREFIX + version;
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        for (BagFile file : payload) {
            if (file.getFilepath().startsWith(prefix + OBJECTS_PREFIX)) {
                objects.add(file.getFilepath().substring((prefix + OBJECTS_PREFIX).length() - 1));
            }
        }
        return objects;
    }

    private void appendObjectsOperations(StringBuilder builder, Bag bag, String pversion, String version, Set<String> pobjects, Set<String> objects) {
        for (String object : objects) {
            if (!pobjects.contains(object)) {
                String bagfilepath = DATA_PREFIX + version + "/objects" + object;
                builder.append("create-object\t").append(bagfilepath).append("\t").append(bag.getChecksums(bagfilepath).get(Algorithm.SHA1)).append("\t").append(object).append("\r\n");
            } else {
                String bagfilepath = DATA_PREFIX + version + "/objects" + object;
                BagFile file = bag.getBagFile(bagfilepath);
                String filehash = bag.getChecksums(bagfilepath).get(Algorithm.SHA1);
                boolean skipupdate = false;
                if (pversion != null) {
                    String oldfilepath = DATA_PREFIX + pversion + "/objects" + object;
                    BagFile oldfile = bag.getBagFile(oldfilepath);
                    String oldfilehash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
                    if (oldfilehash != null && oldfile.getSize() == file.getSize() && oldfilehash.equals(filehash)) {
                        skipupdate = true;
                    }
                }
                if (!skipupdate) {
                    builder.append("update-object\t").append(bagfilepath).append("\t").append(object).append("\r\n");
                }
            }
        }
        for (String object : pobjects) {
            if (!objects.contains(object)) {
                String bagfilepath = DATA_PREFIX + version + "/objects" + object;
                builder.append("delete-object\t").append(bagfilepath).append("\t").append(object).append("\r\n");
            }
        }
    }

    private Set<String> listMetadata(String version, Collection<BagFile> payload) {
        Set<String> metadata = new HashSet<String>();
        String prefix = DATA_PREFIX + version;
        if (!prefix.endsWith("/")) {
            prefix += "/";
        }
        for (BagFile file : payload) {
            if (file.getFilepath().startsWith(prefix + METADATA_PREFIX)) {
                metadata.add(file.getFilepath().substring((prefix + METADATA_PREFIX).length() - 1));
            }
        }
        return metadata;
    }

    private void appendMetadataOperations(StringBuilder builder, Bag bag, String pversion, String version, Set<String> pmetadata, Set<String> metadata) {
        for (String md : metadata) {
            if (!pmetadata.contains(md)) {
                String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
                if (md.lastIndexOf("/") > 0) {
                    builder.append("create-metadata\t").append(bagfilepath).append("\t").append(md.substring(0, md.lastIndexOf("/"))).append("\t").append(md.substring(md.lastIndexOf("/") + 1))
                            .append("\r\n");
                } else {
                    builder.append("create-metadata\t").append(bagfilepath).append("\t/\t").append(md.substring(md.lastIndexOf("/") + 1)).append("\r\n");
                }
            } else {
                String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
                BagFile file = bag.getBagFile(bagfilepath);
                String hash = bag.getChecksums(bagfilepath).get(Algorithm.SHA1);
                boolean skipupdate = false;
                if (pversion != null) {
                    String oldfilepath = DATA_PREFIX + pversion + "/metadata" + md;
                    BagFile oldfile = bag.getBagFile(oldfilepath);
                    String oldhash = bag.getChecksums(oldfilepath).get(Algorithm.SHA1);
                    if (oldhash != null && oldfile.getSize() == file.getSize() && oldhash.equals(hash)) {
                        skipupdate = true;
                    }
                }
                if (!skipupdate) {
                    if (md.lastIndexOf("/") > 0) {
                        builder.append("update-metadata\t").append(bagfilepath).append("\t").append(md.substring(0, md.lastIndexOf("/"))).append("\t").append(md.substring(md.lastIndexOf("/") + 1))
                                .append("\r\n");
                    } else {
                        builder.append("update-metadata\t").append(bagfilepath).append("\t/\t").append(md.substring(md.lastIndexOf("/") + 1)).append("\r\n");
                    }
                }
            }
        }
        for (String md : pmetadata) {
            if (!metadata.contains(md)) {
                String bagfilepath = DATA_PREFIX + version + "/metadata" + md;
                if (md.lastIndexOf("/") > 0) {
                    builder.append("delete-metadata\t").append(bagfilepath).append("\t").append(md.substring(0, md.lastIndexOf("/"))).append("\t").append(md.substring(md.lastIndexOf("/") + 1))
                            .append("\r\n");
                } else {
                    builder.append("delete-metadata\t").append(bagfilepath).append("\t/\t").append(md.substring(md.lastIndexOf("/") + 1)).append("\r\n");
                }
            }
        }
    }

}
