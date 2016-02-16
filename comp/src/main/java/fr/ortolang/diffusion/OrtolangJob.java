package fr.ortolang.diffusion;

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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class OrtolangJob implements Delayed {

	private String action;
	private String target;
	private long timestamp;
	private Map<String, Object> parameters;

	public OrtolangJob(String action, String target, long timestamp) {
		this(action, target, timestamp, new HashMap<String, Object> ());
	}
	
	public OrtolangJob(String action, String target, long timestamp, Map<String, Object> args) {
		this.action = action;
		this.target = target;
		this.timestamp = timestamp;
		this.parameters = args;
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
	
	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
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
	    StringBuilder sb = new StringBuilder();
	    sb.append("{action: ").append(action).append(", target: ").append(target).append(", timestamp: ").append(timestamp).append("}");
	    return sb.toString();
	}

	@Override
	public int compareTo(Delayed obj) {
		if (obj.getClass() != OrtolangJob.class) {
			throw new IllegalArgumentException("Illegal comparision to non-OrtolangJob");
		}
		OrtolangJob other = (OrtolangJob) obj;
		return (int) (this.getTimestamp() - other.getTimestamp());
	}

	@Override
	public long getDelay(TimeUnit unit) {
		long diff = timestamp - System.currentTimeMillis();
		return unit.convert(diff, TimeUnit.MILLISECONDS);
	}

}