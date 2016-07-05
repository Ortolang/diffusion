package fr.ortolang.diffusion.core.wrapper;

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

import fr.ortolang.diffusion.OrtolangObject;
import fr.ortolang.diffusion.core.entity.*;

import javax.persistence.Id;
import java.util.HashMap;
import java.util.Map;

public abstract class OrtolangObjectWrapper {

    @Id
    private String key;
    private String name;
    private String path;
    private Map<String, String> metadatas;

    public OrtolangObjectWrapper(OrtolangObject object, String path, Map<String, String> metadatas) {
        this.key = object.getObjectKey();
        this.key = object.getObjectName();
        this.path = path;
        this.metadatas = metadatas;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public abstract String getType();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(Map<String, String> metadatas) {
        this.metadatas = metadatas;
    }

    public static OrtolangObjectWrapper fromOrtolangObject(OrtolangObject object, String path) {
        if (object instanceof MetadataSource) {
            Map<String, String> metadatas = new HashMap<>();
            for (MetadataElement metadataElement : ((MetadataSource) object).getMetadatas()) {
                metadatas.put(metadataElement.getName(), metadataElement.getKey());
            }
            if (object instanceof DataObject) {
                return new DataObjectWrapper((DataObject) object, path, metadatas);
            } else if (object instanceof Collection) {
                return new CollectionWrapper((Collection) object, path, metadatas);
            } else if (object instanceof Link) {
                return new LinkWrapper((Link) object, path, metadatas);
            }
        }
        return null;
    }
}
