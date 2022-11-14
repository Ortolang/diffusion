package fr.ortolang.diffusion.archive;

import fr.ortolang.diffusion.archive.facile.entity.Validator;

public class ArchiveEntry {
    private String key;
    private String stream;
    private String path;
    private String filename;
    private String encoding;
    private String format;
    private String md5sum;
    private Long size;

    /**
     * Creates an Archive entry containing all informations needed for creating an entry to the archive.
     * @param key the key of the dataobject
     * @param stream the stream of the dataobject
     * @param path the path to the archive
     * @param filename the filename of the entry
     * @param encoding the encoding file
     * @param format the format file
     * @param md5sum the md5 sum of the file
     * @param size the size of the file (octect)
     */
    public ArchiveEntry(String key, String stream, String path, String filename, String encoding, String format, String md5sum, Long size) {
        this.key = key;
        this.stream = stream;
        this.path = path;
        this.filename = filename;
        this.encoding = encoding;
        this.format = format;
        this.md5sum = md5sum;
        this.size = size;
    }

    public static ArchiveEntry newArchiveEntry(String key, String stream, String path, Long size, Validator validator) {
        return new ArchiveEntry(key, stream, path, validator.getFileName(), validator.getEncoding(), 
            validator.getFormat(), validator.getMd5sum(), size);
    }

    public static ArchiveEntry newArchiveEntry(String path, String filename, Long size) {
        return new ArchiveEntry(null, null, path, filename, null, null, null, size);
    }

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getStream() {
        return stream;
    }
    public void setStream(String stream) {
        this.stream = stream;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getFilename() {
        return filename;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getEncoding() {
        return encoding;
    }
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
    public String getMd5sum() {
        return md5sum;
    }
    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }
    public Long getSize() {
        return size;
    }
    public void setSize(Long size) {
        this.size = size;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((stream == null) ? 0 : stream.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((encoding == null) ? 0 : encoding.hashCode());
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((md5sum == null) ? 0 : md5sum.hashCode());
        result = prime * result + ((size == null) ? 0 : size.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ArchiveEntry other = (ArchiveEntry) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (stream == null) {
            if (other.stream != null)
                return false;
        } else if (!stream.equals(other.stream))
            return false;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (encoding == null) {
            if (other.encoding != null)
                return false;
        } else if (!encoding.equals(other.encoding))
            return false;
        if (format == null) {
            if (other.format != null)
                return false;
        } else if (!format.equals(other.format))
            return false;
        if (md5sum == null) {
            if (other.md5sum != null)
                return false;
        } else if (!md5sum.equals(other.md5sum))
            return false;
        if (size == null) {
            if (other.size != null)
                return false;
        } else if (!size.equals(other.size))
            return false;
        return true;
    }
    
    
}
