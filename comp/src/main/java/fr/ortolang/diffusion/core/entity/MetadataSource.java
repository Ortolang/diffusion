package fr.ortolang.diffusion.core.entity;

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
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@MappedSuperclass
public abstract class MetadataSource extends OrtolangObject {

    @Id
    private String id;
    @Version
    private long version;
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String metadatasContent = "";
    @Transient
    private String key;
    private String name;
    private int clock;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getMetadatasContent() {
        return metadatasContent;
    }

    public void setMetadatasContent(String metadatasContent) {
        this.metadatasContent = metadatasContent;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getClock() {
        return clock;
    }

    public void setClock(int clock) {
        this.clock = clock;
    }

    public Set<MetadataElement> getMetadatas() {
        Set<MetadataElement> metadatas = new HashSet<>();
        if (metadatasContent != null && metadatasContent.length() > 0) {
            metadatas.addAll(Arrays.asList(metadatasContent.split("\n")).stream().map(MetadataElement::deserialize).collect(Collectors.toList()));
        }
        return metadatas;
    }

    public void setMetadatas(Set<MetadataElement> metadatas) {
        StringBuilder newMetadatasContent = new StringBuilder();
        for (MetadataElement metadata : metadatas) {
            if (newMetadatasContent.length() > 0) {
                newMetadatasContent.append("\n");
            }
            newMetadatasContent.append(metadata.serialize());
        }
        setMetadatasContent(newMetadatasContent.toString());
    }

    public boolean addMetadata(MetadataElement metadata) {
        if (!containsMetadata(metadata)) {
            if (metadatasContent.length() > 0) {
                metadatasContent += "\n" + metadata.serialize();
            } else {
                metadatasContent = metadata.serialize();
            }
            setMetadatasContent(metadatasContent);
            return true;
        }
        return false;
    }

    public boolean removeMetadata(MetadataElement metadata) {
        if (containsMetadata(metadata)) {
            metadatasContent = metadatasContent.replaceAll("(?m)^(" + metadata.serialize() + ")\n?", "");
            if (metadatasContent.endsWith("\n")) {
                metadatasContent = metadatasContent.substring(0, metadatasContent.length() - 1);
            }
            setMetadatasContent(metadatasContent);
            return true;
        } else {
            return false;
        }
    }

    public boolean containsMetadata(MetadataElement metadata) {
        return metadatasContent.contains(metadata.serialize());
    }

    public boolean containsMetadataName(String name) {
        return metadatasContent.contains(name + "/");
    }

    public boolean containsMetadataKey(String key) {
        return metadatasContent.contains("/" + key);
    }

    public MetadataElement findMetadataByName(String name) {
        Pattern pattern = Pattern.compile("(?s).*(" + name + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
        Matcher matcher = pattern.matcher(metadatasContent);
        if (matcher.matches()) {
            return MetadataElement.deserialize(matcher.group(1));
        }
        return null;
    }

    @Override
    public String getObjectKey() {
        return getKey();
    }

    @Override
    public String getObjectName() {
        return getName();
    }

}
