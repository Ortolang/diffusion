package fr.ortolang.diffusion.statistics.entity;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
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

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@IdClass(StatisticValuePK.class)
@NamedQueries({
    @NamedQuery(name = "countWorkspaceValues", query = "SELECT count(v) FROM WorkspaceStatisticValue v"),
    @NamedQuery(name = "findWorkspaceValues", query = "SELECT v FROM WorkspaceStatisticValue v WHERE v.name = :name ORDER BY v.timestamp DESC"),
    @NamedQuery(name = "findWorkspaceValuesFromTo", query = "SELECT v FROM WorkspaceStatisticValue v WHERE v.name = :name AND v.timestamp > :from AND v.timestamp < :to ORDER BY v.timestamp ASC"),
    @NamedQuery(name = "sumWorkspaceValuesFromTo", query = "SELECT NEW WorkspaceStatisticValue (v.name, 0L, sum(v.visits), sum(v.uniqueVisitors), sum(v.hits), sum(v.downloads), sum(v.singleDownloads)) FROM WorkspaceStatisticValue v WHERE v.name = :name AND v.timestamp > :from AND v.timestamp < :to GROUP BY v.name")
})
@SuppressWarnings("serial")
public class WorkspaceStatisticValue implements Serializable {

    @Id
    private String name;
    @Id
    private long timestamp;

    private long visits;

    private long uniqueVisitors;

    private long hits;

    private long downloads;

    private long singleDownloads;

    public WorkspaceStatisticValue() {
    }

    public WorkspaceStatisticValue(String name, long timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public WorkspaceStatisticValue(String name, long timestamp, long visits, long uniqueVisitors, long hits, long downloads, long singleDownloads) {
        this.name = name;
        this.timestamp = timestamp;
        this.visits = visits;
        this.uniqueVisitors = uniqueVisitors;
        this.hits = hits;
        this.downloads = downloads;
        this.singleDownloads = singleDownloads;
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

    public long getVisits() {
        return visits;
    }

    public void setVisits(long visits) {
        this.visits = visits;
    }

    public void addVisits(long visits) {
        this.visits += visits;
    }

    public long getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }

    public void addUniqueVisitors(long uniqueVisitors) {
        this.uniqueVisitors += uniqueVisitors;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long hits) {
        this.hits = hits;
    }

    public void addHits(long hits) {
        this.hits += hits;
    }

    public long getDownloads() {
        return downloads;
    }

    public void setDownloads(long downloads) {
        this.downloads = downloads;
    }

    public void addDownloads(long downloads) {
        this.downloads += downloads;
    }

    public long getSingleDownloads() {
        return singleDownloads;
    }

    public void setSingleDownloads(long singleDownloads) {
        this.singleDownloads = singleDownloads;
    }

    public void addSingleDownloads(long singleDownloads) {
        this.singleDownloads += singleDownloads;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return visits == 0L && uniqueVisitors == 0L && hits  == 0L && downloads == 0L && singleDownloads == 0L;
    }

    public void copy(WorkspaceStatisticValue value) {
        this.name = value.getName();
        this.timestamp = value.getTimestamp();
        this.visits = value.getVisits();
        this.uniqueVisitors = value.getHits();
        this.hits = value.getHits();
        this.downloads = value.getDownloads();
        this.singleDownloads = value.getSingleDownloads();
    }
}
