package fr.ortolang.diffusion.ftp.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;

import fr.ortolang.diffusion.core.PathBuilder;

public class OrtolangBaseFile implements FtpFile {

    private PathBuilder path;
    private String name;
    private boolean hidden;
    private boolean directory;
    private boolean exists;
    private boolean readable;
    private boolean writeable;
    private boolean removable;
    private String owner;
    private String group;
    private long lastModified;
    private long size;
    
    public OrtolangBaseFile(PathBuilder path, String name) {
        this(path, name, true, false);
    }
    
    public OrtolangBaseFile(PathBuilder path, String name,  boolean directory, boolean exists) {
        this(path, name, directory, exists, false, false, 0, 0);
    }
    
    public OrtolangBaseFile(PathBuilder path, String name, boolean directory, boolean exists, boolean writeable, boolean removable) {
        this(path, name, directory, exists, writeable, removable, 0, 0);
    }
    
    public OrtolangBaseFile(PathBuilder path, String name, boolean directory, boolean exists, boolean writeable, boolean removable, long lastModified, long size) {
        this.path = path;
        this.name = name;
        this.directory = directory;
        this.exists = exists;
        this.writeable = writeable;
        this.removable = removable;
        this.lastModified = lastModified;
        this.size = size;
    }

    public PathBuilder getPath() {
        return path;
    }

    public void setPath(PathBuilder path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    @Override
    public boolean doesExist() {
        return exists;
    }

    public void setExist(boolean exists) {
        this.exists = exists;
    }

    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public long getLastModified() {
        return lastModified;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        this.lastModified = lastModified;
        return true;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public boolean delete() {
        return false;
    }

    
    @Override
    public String getAbsolutePath() {
        return path.build();
    }

    @Override
    public String getGroupName() {
        return group;
    }

    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public String getOwnerName() {
        return owner;
    }

    @Override
    public boolean isFile() {
        return !directory;
    }

    @Override
    public boolean isWritable() {
        return writeable;
    }

    @Override
    public List<FtpFile> listFiles() {
        return Collections.emptyList();
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public boolean move(FtpFile dest) {
        return false;
    }

    @Override
    public InputStream createInputStream(long arg0) throws IOException {
        throw new IOException("unable to create input stream for base file");
    }

    @Override
    public OutputStream createOutputStream(long arg0) throws IOException {
        throw new IOException("unable to create input stream for base file");
    }

    
}
