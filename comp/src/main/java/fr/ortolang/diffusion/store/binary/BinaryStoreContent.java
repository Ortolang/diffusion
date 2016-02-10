package fr.ortolang.diffusion.store.binary;

public class BinaryStoreContent {

    private String path;
    private Type type;
    private long size;
    private long lastModificationDate;
    private String fsName;
    private String fsType;
    private long fsTotalSize;
    private long fsFreeSize;

    public BinaryStoreContent() {
    }

    public String getFsName() {
        return fsName;
    }

    public void setFsName(String name) {
        this.fsName = name;
    }

    public String getPath() {
        return path;
    }

    public Type getType() {
        return type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(long lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFsType() {
        return fsType;
    }

    public void setFsType(String fsType) {
        this.fsType = fsType;
    }

    public long getFsTotalSize() {
        return fsTotalSize;
    }

    public void setFsTotalSize(long fsTotalSize) {
        this.fsTotalSize = fsTotalSize;
    }

    public long getFsFreeSize() {
        return fsFreeSize;
    }

    public void setFsFreeSize(long fsFreeSize) {
        this.fsFreeSize = fsFreeSize;
    }

    enum Type {
        VOLUME, 
        DIRECTORY, 
        FILE
    }

}
