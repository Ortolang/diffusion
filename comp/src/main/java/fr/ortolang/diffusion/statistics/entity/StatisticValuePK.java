package fr.ortolang.diffusion.statistics.entity;

import java.io.Serializable;

@SuppressWarnings("serial")
public class StatisticValuePK implements Serializable {

    private String name;
    private long timestamp;

    public StatisticValuePK() {
    }

    public StatisticValuePK(String name, long timestamp) {
        super();
        this.name = name;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
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
        StatisticValuePK other = (StatisticValuePK) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (timestamp != other.timestamp)
            return false;
        return true;
    }

}
