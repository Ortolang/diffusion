package fr.ortolang.diffusion.core.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;

public class Node {
    
    private static final String SEPARATOR = "/";

    @Id
    public String path;
    public String key;
    public String sha1;
    public Node parent;
    public List<Node> children;
    public List<Node> metadata;
    
    public Node() {
        parent = null;
        children = new ArrayList<Node> ();
        metadata = new ArrayList<Node> ();
    }
    
    public Node (Node parent, String key, String name, String sha1) {
        this();
        this.parent = parent;
        this.path = "/";
        if ( parent != null ) {
            parent.addChildren(this);
            this.path = parent.getPath();
        }
        this.key = key;
        if ( this.path.length() > 1 ) {
            this.path += SEPARATOR;
        }
        this.path += name;
        this.sha1 = sha1;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPath() {
        return path;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public List<Node> getChildren() {
        return children;
    }

    public void setChildren(List<Node> children) {
        this.children = children;
    }
    
    public void addChildren(Node child) {
        this.children.add(child);
    }

    public List<Node> getMetadata() {
        return metadata;
    }

    public void setMetadata(List<Node> metadata) {
        this.metadata = metadata;
    }
    
    public void addMetadata(Node meta) {
        this.metadata.add(meta);
    }
    
//    public StringBuilder buildPath() {
//        StringBuilder builder = null;
//        if ( parent == null ) {
//            builder = new StringBuilder();
//        } else {
//            builder = parent.buildPath();
//        }
//        builder.append("/").append(name);
//        return builder;
//    }
//    
//    public String getPath() {
//        return buildPath().toString();
//    }
    
}
