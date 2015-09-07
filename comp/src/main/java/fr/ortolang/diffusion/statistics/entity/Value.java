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
    @NamedQuery(name = "findValueByName", query = "select v from Value v where v.name = ?1") }
)
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
