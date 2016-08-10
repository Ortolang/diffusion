package fr.ortolang.diffusion.api.message;

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
 * Copyright (C) 2013 - 2016 Ortolang Team
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import fr.ortolang.diffusion.OrtolangObjectInfos;
import fr.ortolang.diffusion.message.entity.Thread;

@XmlRootElement(name = "thread")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreadRepresentation {

    @XmlAttribute(name = "key")
    private String key;
    private String author;
    private String title;
    private long creation;
    private long lastActivity;
    private String workspace;
    private boolean answered;

    public ThreadRepresentation() {
        answered = false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getCreation() {
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public static ThreadRepresentation fromThread(Thread thread) {
        ThreadRepresentation representation = new ThreadRepresentation();
        representation.setKey(thread.getKey());
        representation.setTitle(thread.getTitle());
        representation.setLastActivity(thread.getLastActivity().getTime());
        representation.setWorkspace(thread.getWorkspace());
        if ( thread.getAnswer() != null && thread.getAnswer().length() > 0 ) {
            representation.setAnswered(true);
        }
        return representation;
    }

    public static ThreadRepresentation fromThreadAndInfos(Thread thread, OrtolangObjectInfos infos) {
        ThreadRepresentation representation = fromThread(thread);
        representation.setAuthor(infos.getAuthor());
        representation.setCreation(infos.getCreationDate());
        return representation;
    }

}