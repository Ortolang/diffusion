package fr.ortolang.diffusion.indexing;

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

import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public abstract class OrtolangIndexableContent {

    private String index;

    private String type;

    private String id;

    private String content;

    private boolean update;

    private String script;

    private Map<String, Object> scriptParams;

    public OrtolangIndexableContent() {
        this(null, null, null);
    }

    public OrtolangIndexableContent(String index, String type, String id) {
        this.index = index;
        this.type = type;
        this.id = id;
        this.update = false;
        this.script = null;
        this.scriptParams = Collections.emptyMap();
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    protected void setContent(Map<String, Object> content) throws IndexableContentParsingException {
        setContent(new JSONObject(content).toString());
    }

    protected void setContent(String json) throws IndexableContentParsingException {
        this.content = OrtolangIndexableContentParser.parse(json);
    }

    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public String getScript() {
        return script;
    }

    protected void setScript(String script) {
        update = true;
        this.script = script;
    }

    public Map<String, Object> getScriptParams() {
        return scriptParams;
    }

    protected void setScriptParams(Map<String, Object> scriptParams) {
        this.scriptParams = scriptParams;
    }

    public abstract Object[] getMapping();

    public boolean isEmpty() {
        return (script == null || script.isEmpty()) && (content == null || content.isEmpty());
    }
}
