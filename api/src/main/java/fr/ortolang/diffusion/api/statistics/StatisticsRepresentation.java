package fr.ortolang.diffusion.api.statistics;


public class StatisticsRepresentation {

    private String key;
    private long[][] values;

    public StatisticsRepresentation() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long[][] getValues() {
        return values;
    }

    public void setValues(long[][] values) {
        this.values = values;
    }

}
