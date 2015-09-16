package fr.ortolang.diffusion.runtime.entity;

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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Type;

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.runtime.RuntimeService;

@Entity
@Table(indexes = { @Index(columnList = "initier,state") })
@NamedQueries({ @NamedQuery(name = "findAllProcesses", query = "select p from Process p"),
		@NamedQuery(name = "findProcessByState", query = "select p from Process p where p.state = :state"),
		@NamedQuery(name = "findProcessByInitier", query = "select p from Process p where p.initier = :initier"),
		@NamedQuery(name = "findProcessByIniterAndState", query = "select p from Process p where p.state = :state and p.initier = :initier"),
		@NamedQuery(name = "findProcessByWorkspace", query = "select p from Process p where p.workspace = :workspace"),
		@NamedQuery(name = "findProcessByWorkspaceAndState", query = "select p from Process p where p.workspace = :workspace and p.state = :state")})
@SuppressWarnings("serial")
public class Process extends OrtolangObject {

	public static final String OBJECT_TYPE = "process";
	
	public static final String INITIER_VAR_NAME = "initier";
	public static final String WSKEY_VAR_NAME = "wskey";

	public static enum State {
		PENDING, SUBMITTED, RUNNING, SUSPENDED, ABORTED, COMPLETED
	}

	@Id
	private String id;
	@Version
	private long version;
	@Transient
	private String key;
	private String name;
	private String type;
	private String initier;
	private String workspace;
	private State state;
	private int progress;
	private String activity;
	private long start;
	private long stop;
	@Lob
	@Type(type = "org.hibernate.type.TextType")
	private String log;

	public Process() {
		log = "";
		progress = 0;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInitier() {
		return initier;
	}

	public void setInitier(String initier) {
		this.initier = initier;
	}
	
	public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public void appendLog(String log) {
		this.log += log + "\r\n";
	}

	public int getProgress() {
		return progress;
	}

	public void setProgress(int progress) {
		this.progress = progress;
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}
	
	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}
	
	public long getStop() {
		return stop;
	}

	public void setStop(long stop) {
		this.stop = stop;
	}

	@Override
	public String getObjectName() {
		return getName();
	}

	@Override
	public String getObjectKey() {
		return getKey();
	}

	@Override
	public OrtolangObjectIdentifier getObjectIdentifier() {
		return new OrtolangObjectIdentifier(RuntimeService.SERVICE_NAME, Process.OBJECT_TYPE, id);
	}
}
