package fr.ortolang.diffusion.api.workspace;

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

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import javax.ws.rs.FormParam;
import java.io.InputStream;

public class WorkspaceElementFormRepresentation {

    @FormParam("path")
    @PartType("text/plain")
    private String path = null;

    @FormParam("type")
    @PartType("text/plain")
    private String type = null;

    @FormParam("name")
    @PartType("text/plain")
    private String name = "";

    @FormParam("description")
    @PartType("text/plain")
    private String description = "";

    @FormParam("format")
    @PartType("text/plain")
    private String format = "";

    @FormParam("target")
    @PartType("text/plain")
    private String target = "";

    @FormParam("preview")
    @PartType("application/octet-stream")
    private InputStream preview = null;
    private String previewHash = "";

    @FormParam("stream")
    @PartType("application/octet-stream")
    private InputStream stream = null;
    private String streamHash = "";

    @FormParam("streamFilename")
    @PartType("text/plain")
    private String streamFilename = null;

    public WorkspaceElementFormRepresentation() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public InputStream getPreview() {
        return preview;
    }

    public void setPreview(InputStream preview) {
        this.preview = preview;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public String getPreviewHash() {
        return previewHash;
    }

    public void setPreviewHash(String previewHash) {
        this.previewHash = previewHash;
    }

    public String getStreamHash() {
        return streamHash;
    }

    public void setStreamHash(String streamHash) {
        this.streamHash = streamHash;
    }

    public String getStreamFilename() {
        return streamFilename;
    }

    public void setStreamFilename(String streamFilename) {
        this.streamFilename = streamFilename;
    }
}
