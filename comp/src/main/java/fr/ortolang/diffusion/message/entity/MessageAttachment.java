package fr.ortolang.diffusion.message.entity;

import javax.persistence.Embeddable;

@Embeddable
public class MessageAttachment {
    
    private String name;
    private String type;
    private long size;
    private String hash;

    public MessageAttachment() {
    }

    public MessageAttachment(String name, String type, long size, String hash) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{Name:").append(getName());
        builder.append(",Type:").append(getType());
        builder.append(",Size:").append(getSize());
        builder.append(",Hash:").append(getHash()).append("}");
        return builder.toString();
    }

}
