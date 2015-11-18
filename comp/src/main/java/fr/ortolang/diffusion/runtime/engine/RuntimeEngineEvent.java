package fr.ortolang.diffusion.runtime.engine;

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

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Set;

import org.activiti.engine.task.IdentityLink;

@SuppressWarnings("serial")
public class RuntimeEngineEvent implements Serializable {

    public static final String MESSAGE_PROPERTY_NAME = "event_object";

    public enum Type {
        PROCESS_START, PROCESS_ABORT, PROCESS_COMPLETE, PROCESS_ACTIVITY_STARTED, PROCESS_ACTIVITY_PROGRESS, PROCESS_ACTIVITY_COMPLETED, PROCESS_ACTIVITY_ERROR, PROCESS_LOG, PROCESS_TRACE, TASK_CREATED, TASK_ASSIGNED, TASK_COMPLETED
    }

    private long timestamp;
    private Type type;
    private String pid;
    private String tid;
    private String activityName;
    private int activityProgress;
    private String message;
    private String trace;
    private Set<IdentityLink> candidates;
    private String assignee;
    
    private RuntimeEngineEvent() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getActivityProgress() {
        return activityProgress;
    }

    public void setActivityProgress(int activityProgress) {
        this.activityProgress = activityProgress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public Set<IdentityLink> getCandidates() {
        return candidates;
    }

    public void setCandidates(Set<IdentityLink> candidates) {
        this.candidates = candidates;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public static RuntimeEngineEvent createProcessStartEvent(String pid) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_START);
        event.setPid(pid);
        return event;
    }

    public static RuntimeEngineEvent createProcessAbortEvent(String pid, String message) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_ABORT);
        event.setPid(pid);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createProcessCompleteEvent(String pid) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_COMPLETE);
        event.setPid(pid);
        return event;
    }

    public static RuntimeEngineEvent createProcessLogEvent(String pid, String message) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_LOG);
        event.setPid(pid);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createProcessTraceEvent(String pid, String message, Throwable throwable) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_TRACE);
        event.setPid(pid);
        event.setMessage(message);
        if (throwable != null) {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            event.setTrace(writer.toString());
        } else {
            event.setTrace("");
        }
        return event;
    }

    public static RuntimeEngineEvent createProcessActivityStartEvent(String pid, String name, String message) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_ACTIVITY_STARTED);
        event.setPid(pid);
        event.setActivityName(name);
        event.setActivityProgress(0);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createProcessActivityCompleteEvent(String pid, String name, String message) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_ACTIVITY_COMPLETED);
        event.setPid(pid);
        event.setActivityName(name);
        event.setActivityProgress(100);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createProcessActivityErrorEvent(String pid, String name, String message) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_ACTIVITY_ERROR);
        event.setPid(pid);
        event.setActivityName(name);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createProcessActivityProgressEvent(String pid, String name, String message, int progression) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.PROCESS_ACTIVITY_PROGRESS);
        event.setPid(pid);
        event.setActivityName(name);
        event.setActivityProgress(progression);
        event.setMessage(message);
        return event;
    }

    public static RuntimeEngineEvent createTaskCreatedEvent(String pid, String tid, String name, Set<IdentityLink> candidates) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.TASK_CREATED);
        event.setPid(pid);
        event.setTid(tid);
        event.setActivityName(name);
        event.setCandidates(candidates);
        return event;
    }

    public static RuntimeEngineEvent createTaskAssignedEvent(String pid, String tid, String name, String assignee) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.TASK_ASSIGNED);
        event.setPid(pid);
        event.setTid(tid);
        event.setActivityName(name);
        event.setAssignee(assignee);
        return event;
    }

    public static RuntimeEngineEvent createTaskCompletedEvent(String pid, String tid, String name, String assignee) {
        RuntimeEngineEvent event = new RuntimeEngineEvent();
        event.setTimestamp(System.currentTimeMillis());
        event.setType(Type.TASK_COMPLETED);
        event.setPid(pid);
        event.setTid(tid);
        event.setActivityName(name);
        event.setAssignee(assignee);
        return event;
    }
}
