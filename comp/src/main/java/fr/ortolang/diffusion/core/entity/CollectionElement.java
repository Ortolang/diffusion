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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Comparator;
import java.util.StringTokenizer;

@SuppressWarnings("serial")
public class CollectionElement implements Serializable, Comparable<CollectionElement> {

    private String type;
    private String name;
    private long modification;
    private long size;
    private String mimeType;
    private String key;

    public CollectionElement() {
    }

    public CollectionElement(String type, String name, long modification, long size, String mimeType, String key) {
        this.type = type;
        this.name = name;
        this.modification = modification;
        this.size = size;
        this.mimeType = mimeType;
        this.key = key;
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

    public long getModification() {
        return modification;
    }

    public void setModification(long modification) {
        this.modification = modification;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String serialize() {
        return this.getType() + "\t" + this.getName() + "\t" + this.getModification() + "\t" + this.getSize() + "\t" + this.getMimeType() + "\t" + this.getKey();
    }

    public static CollectionElement deserialize(String serializedElement) {
        if (serializedElement == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(serializedElement, "\t");
        return new CollectionElement(tokenizer.nextToken(), tokenizer.nextToken(), Long.parseLong(tokenizer.nextToken()), Long.parseLong(tokenizer.nextToken()), tokenizer.nextToken(),
                tokenizer.nextToken());
    }

    @Override
    public String toString() {
        return "CollectionElement{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", modification=" + modification +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((mimeType == null) ? 0 : mimeType.hashCode());
        result = prime * result + (int) (modification ^ (modification >>> 32));
        result = prime * result + (int) (size ^ (size >>> 32));
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CollectionElement other = (CollectionElement) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (mimeType == null) {
            if (other.mimeType != null)
                return false;
        } else if (!mimeType.equals(other.mimeType))
            return false;
        if (modification != other.modification)
            return false;
        if (size != other.size)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    @Override
    public int compareTo(@Nonnull CollectionElement element) {
        if (this.getType().equals(element.getType())) {
            return this.getName().compareToIgnoreCase(element.getName());
        } else {
            return this.getType().compareToIgnoreCase(element.getType());
        }
    }

    public static Comparator<CollectionElement> ElementNameAscComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return element1.getName().compareToIgnoreCase(element2.getName());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementNameDescComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return element2.getName().compareToIgnoreCase(element1.getName());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementSizeAscComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return (int) (element1.getSize() - element2.getSize());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementSizeDescComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return (int) (element2.getSize() - element1.getSize());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementTypeAscComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return element1.getMimeType().compareToIgnoreCase(element2.getMimeType());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementTypeDescComparator = (element1, element2) -> {
        if (element2.getType().equals(element1.getType())) {
            return element2.getMimeType().compareToIgnoreCase(element1.getMimeType());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementDateAscComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return (int) (element1.getModification() - element2.getModification());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

    public static Comparator<CollectionElement> ElementDateDescComparator = (element1, element2) -> {
        if (element1.getType().equals(element2.getType())) {
            return (int) (element2.getModification() - element1.getModification());
        } else {
            return element1.getType().compareToIgnoreCase(element2.getType());
        }
    };

}
