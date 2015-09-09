package fr.ortolang.diffusion.statistics.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@IdClass(ValuePK.class)
@NamedQueries({ 
    @NamedQuery(name = "findValuesForName", query = "SELECT v FROM Value v WHERE v.name = :name ORDER BY v.timestamp DESC"),
    @NamedQuery(name = "findValuesForNameFromTo", query = "SELECT v FROM Value v WHERE v.name = :name AND v.timestamp > :from AND v.timestamp < :to ORDER BY v.timestamp ASC")
})
@SuppressWarnings("serial")
public class Value implements Serializable {
    
    @Id
    private String name;
    @Id
    private long timestamp;
    private String value;
    
    public Value() {
    }
    
    public Value(String name, long timestamp, String value) {
        this.name = name;
        this.timestamp = timestamp;
        this.value = value;
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}
