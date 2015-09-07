package fr.ortolang.diffusion.statistics.entity;

public class Monitor {

    private String service;
    private String name;
    private Type type;

    public Monitor(String service, String name, Type type) {
        this.service = service;
        this.name = name;
        this.type = type;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public enum Type {

        STATUS, GAUGE_INT, GAUGE_LONG, GAUGE_DOUBLE, VALUE_INT, VALUE_LONG, VALUE_DOUBLE

    }

}
