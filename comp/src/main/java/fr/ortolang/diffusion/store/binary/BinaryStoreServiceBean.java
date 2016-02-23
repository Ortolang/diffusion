package fr.ortolang.diffusion.store.binary;

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectSize;
import fr.ortolang.diffusion.store.binary.BinaryStoreContent.Type;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStream;
import fr.ortolang.diffusion.store.binary.hash.HashedFilterInputStreamFactory;
import fr.ortolang.diffusion.store.binary.hash.SHA1FilterInputStreamFactory;

/**
 * Local FileSystem based implementation of the BinaryStoreService.<br>
 * <br>
 * This implementation store all contents in the provided base folder in the local file system using a SHA1 hash generator.
 * 
 * @author Jerome Blanchard (jayblanc@gmail.com)
 * @version 1.0
 */
@Local(BinaryStoreService.class)
@Startup
@Singleton(name = BinaryStoreService.SERVICE_NAME)
@SecurityDomain("ortolang")
@Lock(LockType.READ)
@PermitAll
public class BinaryStoreServiceBean implements BinaryStoreService {

    public static final String DEFAULT_BINARY_HOME = "binary-store";
    public static final int DISTINGUISH_SIZE = 2;

    private static final Logger LOGGER = Logger.getLogger(BinaryStoreServiceBean.class.getName());

    private static final String[] OBJECT_TYPE_LIST = new String[] {};
    private static final String[] OBJECT_PERMISSIONS_LIST = new String[] {};

    private static final String WORK = "work";
    private static final String COLLIDE = "collide";

    private HashedFilterInputStreamFactory factory;
    private Path base;
    private Path working;
    private Path collide;

    public BinaryStoreServiceBean() {
    }

    @PostConstruct
    public void init() {
        this.base = Paths.get(OrtolangConfig.getInstance().getHomePath().toString(), DEFAULT_BINARY_HOME);
        this.working = Paths.get(base.toString(), WORK);
        this.collide = Paths.get(base.toString(), COLLIDE);
        this.factory = new SHA1FilterInputStreamFactory();
        LOGGER.log(Level.FINEST, "Initializing service with base folder: " + base);
        try {
            Files.createDirectories(base);
            Files.createDirectories(working);
            Files.createDirectories(collide);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "unable to initialize binary store", e);
        }
    }

    public HashedFilterInputStreamFactory getHashedFilterInputStreamFactory() {
        return factory;
    }

    public void setHashedFilterInputStreamFactory(HashedFilterInputStreamFactory factory) {
        this.factory = factory;
    }

    public Path getBase() {
        return base;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public boolean contains(String identifier) throws BinaryStoreServiceException {
        try {
            Path path = getPathForIdentifier(identifier);
            return Files.exists(path);
        } catch (DataNotFoundException e) {
            return false;
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public InputStream get(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            return Files.newInputStream(path);
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public File getFile(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            return path.toFile();
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long size(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            return Files.size(path);
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String type(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            Tika tika = new Tika();
            return tika.detect(path.toFile());
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String type(String identifier, String filename) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try (InputStream is = Files.newInputStream(path)) {
            Tika tika = new Tika();
            String type;
            if (Files.size(path) < 50000000) {
                LOGGER.log(Level.FINEST, "file size is not too large, trying to detect also containers");
                TikaInputStream tis = TikaInputStream.get(is);
                type = tika.detect(tis, filename);
            } else {
                LOGGER.log(Level.FINEST, "file size is TOO large, does not detect types inside containers");
                type = tika.detect(is, filename);
            }
            return type;
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String extract(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            Tika tika = new Tika();
            tika.setMaxStringLength(20000000);
            return tika.parseToString(path.toFile());
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String put(InputStream content) throws BinaryStoreServiceException, DataCollisionException {
        try {
            HashedFilterInputStream input = factory.getHashedFilterInputStream(content);
            try {
                Path tmpfile = Paths.get(working.toString(), Long.toString(System.nanoTime()));

                Files.copy(input, tmpfile);
                LOGGER.log(Level.FINE, "content stored in local temporary file: " + tmpfile.toString());
                String hash = input.getHash();
                LOGGER.log(Level.FINE, "content based generated sha1 hash: " + hash);

                String digit = hash.substring(0, DISTINGUISH_SIZE);
                Path volume = Paths.get(base.toString(), BinaryStoreVolumeMapper.getVolume(digit));
                Path parent = Paths.get(base.toString(), BinaryStoreVolumeMapper.getVolume(digit), digit);
                Path file = Paths.get(base.toString(), BinaryStoreVolumeMapper.getVolume(digit), digit, hash);
                if (!Files.exists(volume)) {
                    Files.createDirectory(volume);
                }
                if (!Files.exists(parent)) {
                    Files.createDirectory(parent);
                }
                if (!Files.exists(file)) {
                    Files.move(tmpfile, file);
                    LOGGER.log(Level.FINE, "content moved in local definitive file: " + file.toString());
                } else {
                    LOGGER.log(Level.INFO, "a file with same hash already exists, trying to detect collision");
                    try (InputStream input1 = Files.newInputStream(file); InputStream input2 = Files.newInputStream(tmpfile)) {
                        if (IOUtils.contentEquals(input1, input2)) {
                            Files.delete(tmpfile);
                        } else {
                            LOGGER.log(Level.SEVERE, "BINARY COLLISION DETECTED - storing colliding files in dedicated folder");
                            Files.copy(file, Paths.get(collide.toString(), hash + ".origin"));
                            Files.move(tmpfile, Paths.get(collide.toString(), hash + ".colliding"));
                            throw new DataCollisionException();
                        }
                    }
                }
                return hash;
            } catch (IOException | VolumeNotFoundException e) {
                throw new BinaryStoreServiceException(e);
            } finally {
                IOUtils.closeQuietly(input);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void check(String identifier) throws BinaryStoreServiceException, DataNotFoundException, DataCorruptedException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        String check;
        try (InputStream input = Files.newInputStream(path)) {
            check = generate(input);
        } catch (IOException e) {
            throw new BinaryStoreServiceException(e);
        }
        if (!check.equals(identifier)) {
            throw new DataCorruptedException("The object with id [" + identifier + "] is CORRUPTED. The stored object's content has generate a wrong identifier [" + check + "]");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void delete(String identifier) throws BinaryStoreServiceException, DataNotFoundException {
        Path path = getPathForIdentifier(identifier);
        if (!Files.exists(path)) {
            throw new DataNotFoundException("Unable to find an object with id [" + identifier + "] in the storage");
        }
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new BinaryStoreServiceException(e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String generate(InputStream content) throws BinaryStoreServiceException {
        try {
            HashedFilterInputStream input = factory.getHashedFilterInputStream(content);
            byte[] buffer = new byte[10240];
            while (input.read(buffer) >= 0) {
            }
            return input.getHash();
        } catch (Exception e) {
            throw new BinaryStoreServiceException("Unable to generate a hash for this content: " + e.getMessage(), e);
        }
    }

    private Path getPathForIdentifier(String identifier) throws DataNotFoundException {
        String digit = identifier.substring(0, DISTINGUISH_SIZE);
        try {
            return Paths.get(base.toString(), BinaryStoreVolumeMapper.getVolume(digit), digit, identifier);
        } catch (VolumeNotFoundException e) {
            throw new DataNotFoundException(e);
        }
    }

    private long getStoreNbFiles() throws IOException {
        long nbfiles = Files.walk(base).count();
        return nbfiles;
    }

    private long getStoreSize() throws IOException {
        long size = Files.walk(base).mapToLong(this::size).sum();
        return size;
    }

    private long size(Path p) {
        try {
            return Files.size(p);
        } catch (Exception e) {
            return 0;
        }
    }

    // System methods

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<BinaryStoreContent> systemBrowse(String name, String prefix) throws BinaryStoreServiceException {
        if (name == null || name.length() == 0) {
            List<BinaryStoreContent> vinfos = new ArrayList<BinaryStoreContent>();
            List<String> vnames = new ArrayList<String>();
            vnames.addAll(BinaryStoreVolumeMapper.listVolumes());
            vnames.add(WORK);
            for (String vname : vnames) {
                try {
                    BinaryStoreContent volume = new BinaryStoreContent();
                    Path vpath = Paths.get(base.toString(), vname);
                    FileStore vstore = Files.getFileStore(vpath);
                    volume.setPath(vname);
                    volume.setType(Type.VOLUME);
                    volume.setFsName(vstore.name());
                    volume.setFsType(vstore.type());
                    volume.setFsTotalSize(vstore.getTotalSpace());
                    volume.setFsFreeSize(vstore.getUsableSpace());
                    volume.setSize(Files.size(vpath));
                    volume.setLastModificationDate(Files.getLastModifiedTime(vpath).toMillis());
                    vinfos.add(volume);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Unable to retrieve binary store volume information for volume: " + vname);
                }
            }
            return vinfos;
        } else {
            try {
                if (prefix == null) {
                    prefix = "";
                }
                Path vpath = Paths.get(base.toString(), name, prefix);
                if (!Files.exists(vpath)) {
                    throw new BinaryStoreServiceException("volume name does not point to an existing file or a directory");
                }
                return Files.list(vpath).map(this::pathToContent).collect(Collectors.toList());
            } catch (IOException e) {
                throw new BinaryStoreServiceException(e);
            }    
        }
    }
    
    private BinaryStoreContent pathToContent(Path path) {
        BinaryStoreContent content = new BinaryStoreContent();
        content.setPath(base.relativize(path).toString());
        if (Files.isDirectory(path)) {
            content.setType(Type.DIRECTORY);
        } else {
            content.setType(Type.FILE);
        }
        try {
            content.setSize(Files.size(path));
            content.setLastModificationDate(Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to retrieve binary store content information for path: " + path);
        }
        return content;
    }

    // Service methods

    @Override
    public String getServiceName() {
        return BinaryStoreService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        Map<String, String> infos = new HashMap<String, String>();
        infos.put(INFO_PATH, this.base.toString());
        try {
            infos.put(INFO_FILES, Long.toString(getStoreNbFiles()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_FILES, e);
        }
        try {
            infos.put(INFO_SIZE, Long.toString(getStoreSize()));
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "unable to collect info: " + INFO_SIZE, e);
        }
        return infos;
    }

    @Override
    public String[] getObjectTypeList() {
        return OBJECT_TYPE_LIST;
    }

    @Override
    public String[] getObjectPermissionsList(String type) throws OrtolangException {
        return OBJECT_PERMISSIONS_LIST;
    }

    @Override
    public OrtolangObject findObject(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }

    @Override
    public OrtolangObjectSize getSize(String key) throws OrtolangException {
        throw new OrtolangException("this service does not managed any object");
    }
}
