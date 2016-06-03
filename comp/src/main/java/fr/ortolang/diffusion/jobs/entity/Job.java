package fr.ortolang.diffusion.jobs.entity;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * *
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 * *
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import javax.persistence.*;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

@Entity
@NamedQueries({
        @NamedQuery(name = "countAllJobs", query = "SELECT count(j) FROM Job j"),
        @NamedQuery(name = "listAllJobs", query = "SELECT j FROM Job j ORDER BY j.id"),
        @NamedQuery(name = "countJobsOfType", query = "SELECT count(j) FROM Job j WHERE :type = j.type"),
        @NamedQuery(name = "listJobsOfType", query = "SELECT j FROM Job j WHERE :type = j.type ORDER BY j.id")
})
public class Job implements Serializable, Delayed {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String type;
    private String action;
    private String target;
    private long timestamp;
    @ElementCollection
    private Map<String, String> parameters;


    public Job() {
    }

    public Job(String type, String action, String target, long timestamp, Map<String, String> args) {
        this.type = type;
        this.action = action;
        this.target = target;
        this.timestamp = timestamp;
        this.parameters = args;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public boolean containsParameter(String name) {
        return parameters.containsKey(name);
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", action='" + action + '\'' +
                ", target='" + target + '\'' +
                ", timestamp=" + timestamp +
                "} " + super.toString();
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = timestamp - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed obj) {
        if (!(obj instanceof Job)) {
            throw new IllegalArgumentException("Illegal comparison to non-Job");
        }
        Job other = (Job) obj;
        return (int) (this.getTimestamp() - other.getTimestamp());
    }
}
