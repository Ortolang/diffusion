package fr.ortolang.diffusion.runtime.engine.task;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.core.*;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineEvent;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTask;
import fr.ortolang.diffusion.runtime.engine.RuntimeEngineTaskException;
import fr.ortolang.diffusion.security.authorisation.AccessDeniedException;
import fr.ortolang.diffusion.store.binary.DataCollisionException;
import org.activiti.engine.delegate.DelegateExecution;

import javax.transaction.Status;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ImportZipTask extends RuntimeEngineTask {

    private static final Logger LOGGER = Logger.getLogger(ImportZipTask.class.getName());
    private static final List<Pattern> IGNORED_FILES;

    public static final String NAME = "Import Zip Content";

    static {
        List<Pattern> patterns = new ArrayList<>();
        for (String regex : OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.ZIP_IGNORED_FILES).split(",")) {
            patterns.add(Pattern.compile(regex));
        }
        IGNORED_FILES = patterns;
    }

    public ImportZipTask() {
    }

    @Override
    public void executeTask(DelegateExecution execution) throws RuntimeEngineTaskException {
        checkParameters(execution);
        String wskey = execution.getVariable(WORKSPACE_KEY_PARAM_NAME, String.class);
        String zippath = execution.getVariable(ZIP_PATH_PARAM_NAME, String.class);
        String root = execution.getVariable(ZIP_ROOT_PARAM_NAME, String.class);
        PathBuilder rootPath;
        try {
            rootPath = PathBuilder.fromPath(root);
        } catch (InvalidPathException e) {
            throw new RuntimeEngineTaskException("parameter " + ZIP_ROOT_PARAM_NAME + " value " + root + " is not a valid path");
        }
        StringBuilder report = new StringBuilder();
        boolean overwrite = false;
        if (execution.hasVariable(ZIP_OVERWRITE_PARAM_NAME)) {
            overwrite = Boolean.parseBoolean(execution.getVariable(ZIP_OVERWRITE_PARAM_NAME, String.class));
            if ( overwrite ) {
                LOGGER.log(Level.FINE, "zip import will OVERWRITE existing files");
                report.append("zip import with OVERWRITING existing files\r\n");
            }
        }

        try {
            if (getUserTransaction().getStatus() == Status.STATUS_NO_TRANSACTION) {
                LOGGER.log(Level.FINE, "starting new user transaction.");
                getUserTransaction().begin();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to start new user transaction", e);
        }

        LOGGER.log(Level.FINE, "- starting import zip");
        try {
            Set<String> cache = new HashSet<String>();
            ZipFile zip = new ZipFile(zippath);
            boolean partial = false;
            boolean ignore;
            boolean needcommit;
            long tscommit = System.currentTimeMillis();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
                ZipEntry entry = e.nextElement();
                LOGGER.log(Level.FINE, " zip entry found: " + entry.getName());
                needcommit = false;
                ignore = false;
                try {
                    for (Pattern pattern : IGNORED_FILES) {
                        if (pattern.matcher(entry.getName()).matches()) {
                            ignore = true;
                            break;
                        }
                    }
                    if (ignore) {
                        continue;
                    }
                    if (!entry.isDirectory()) {
                        PathBuilder opath = rootPath.clone().path(entry.getName());
                        try {
                            getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, opath.build());
                            if ( overwrite ) {
                                LOGGER.log(Level.FINE, " updating object at path: " + opath.build());
                                try {
                                    InputStream is = zip.getInputStream(entry);
                                    String hash = getCoreService().put(is);
                                    is.close();
                                    getCoreService().updateDataObject(wskey, opath.build(), hash);
                                    report.append("[DONE] object updated at path: " + opath.build() + "\r\n");
                                } catch ( InvalidPathException | DataCollisionException | KeyNotFoundException | PathNotFoundException e4 ) {
                                    partial = true;
                                    report.append("[ERROR] object updated failed for path: " + opath.build() + "\r\n\t-> message: " + e4.getMessage() + "\r\n");
                                }
                            }
                        } catch ( PathNotFoundException e3 ) {
                            LOGGER.log(Level.FINE, " creating object at path: " + opath.build());
                            try {
                                InputStream is = zip.getInputStream(entry);
                                String hash = getCoreService().put(is);
                                is.close();
                                PathBuilder oppath = opath.clone().parent();
                                if (!oppath.isRoot() && !cache.contains(oppath.build())) {
                                    String[] parents = oppath.buildParts();
                                    String current = "";
                                    for (int i = 0; i < parents.length; i++) {
                                        current += "/" + parents[i];
                                        if (!cache.contains(current)) {
                                            try {
                                                getCoreService().resolveWorkspacePath(wskey, Workspace.HEAD, current);
                                            } catch (InvalidPathException | PathNotFoundException e2) {
                                                getCoreService().createCollection(wskey, current);
                                                report.append("[DONE] collection created at path: " + current + "\r\n");
                                            }
                                            cache.add(current);
                                        }
                                    }
                                }
                                String current = opath.build();
                                getCoreService().createDataObject(wskey, current, hash);
                                report.append("[DONE] data object created at path: " + current + "\r\n");
                            } catch ( InvalidPathException | DataCollisionException | KeyNotFoundException | PathNotFoundException | PathAlreadyExistsException e4 ) {
                                partial = true;
                                report.append("[ERROR] create object failed for path: " + opath.build() + "\r\n\t-> message: " + e4.getMessage() + "\r\n");
                            }
                        }
                    }
                } catch (InvalidPathException | CoreServiceException | AccessDeniedException e2) {
                    partial = true;
                    report.append("[ERROR] unexpected error: " + e2.getMessage() + "\r\n");
                }
                if ( System.currentTimeMillis() - tscommit > 300000 ) {
                    LOGGER.log(Level.FINE, "current transaction exceed 5min, need commit.");
                    needcommit = true;
                }
                try {
                    if (needcommit && getUserTransaction().getStatus() == Status.STATUS_ACTIVE) {
                        LOGGER.log(Level.FINE, "commiting active user transaction.");
                        getUserTransaction().commit();
                        tscommit = System.currentTimeMillis();
                        getUserTransaction().begin();
                    }
                } catch (Exception e2) {
                    LOGGER.log(Level.SEVERE, "unable to commit active user transaction", e2);
                }
            }
            zip.close();
            if ( partial ) {
                throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Some objects has not been imported (see trace for detail)"));
            }
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessLogEvent(execution.getProcessBusinessKey(), "Import Zip content done"));
            throwRuntimeEngineEvent(RuntimeEngineEvent.createProcessTraceEvent(execution.getProcessBusinessKey(), report.toString(), null));
        } catch (IOException e1) {
            throw new RuntimeEngineTaskException("error reading zip file: " + e1.getMessage());
        }
    }

    @Override
    public String getTaskName() {
        return NAME;
    }

    private void checkParameters(DelegateExecution execution) throws RuntimeEngineTaskException {
        if (!execution.hasVariable(WORKSPACE_KEY_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + WORKSPACE_KEY_PARAM_NAME + " is not set");
        }
        if (!execution.hasVariable(ZIP_PATH_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + ZIP_PATH_PARAM_NAME + " is not set");
        }
        if (!execution.hasVariable(ZIP_ROOT_PARAM_NAME)) {
            throw new RuntimeEngineTaskException("execution variable " + ZIP_ROOT_PARAM_NAME + " is not set");
        }
    }

}
