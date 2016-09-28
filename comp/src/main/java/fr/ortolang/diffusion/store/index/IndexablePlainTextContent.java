package fr.ortolang.diffusion.store.index;

import org.apache.lucene.document.FieldType;

import java.util.ArrayList;
import java.util.List;

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

public class IndexablePlainTextContent {

    private String name = "";
    private StringBuilder sb;
    private List<IndexablePlainTextContentProperty> properties;
    private long boost;

    public IndexablePlainTextContent() {
        sb = new StringBuilder();
        properties = new ArrayList<>();
        boost = 1L;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addContentPart(String content) {
        sb.append(content).append(" ");
    }

    public void addContentPart(String name, String value) {
        sb.append(value).append(" ");
        addProperty(name, value);
    }

    public void addContentPart(String name, String value, FieldType fieldType) {
        sb.append(value).append(" ");
        addProperty(name, value, fieldType);
    }

    public List<IndexablePlainTextContentProperty> getProperties() {
        return properties;
    }

    public void addProperty(String name, String value) {
        properties.add(new IndexablePlainTextContentProperty(name, value));
    }

    public void addProperty(String name, String value, FieldType fieldType) {
        properties.add(new IndexablePlainTextContentProperty(name, value, fieldType));
    }

    public long getBoost() {
        return boost;
    }

    public void setBoost(long boost) {
        this.boost = boost;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
