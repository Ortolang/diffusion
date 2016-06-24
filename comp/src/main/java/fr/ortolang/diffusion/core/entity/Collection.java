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

import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.core.CoreService;

import javax.persistence.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@SuppressWarnings("serial")
public class Collection extends MetadataSource {

    public static final String OBJECT_TYPE = "collection";
    public static final String MIME_TYPE = "ortolang/collection";

    private static final int MAX_SEGMENT_SIZE = 7500;

    private boolean root;
    @Column(length=8000)
    @ElementCollection(fetch=FetchType.EAGER)
    private Set<String> segments;

    @Transient
    private Set<CollectionElement> elements = null;

    public Collection() {
        segments = new HashSet<String>();
    }

    public boolean isRoot() {
        return root;
    }

    public void setRoot(boolean root) {
        this.root = root;
    }

    public void setSegments(Set<String> segments) {
        this.segments = segments;
    }

    public Set<String> getSegments() {
        return segments;
    }

    public boolean isEmpty() {
        if ( elements != null ) {
            return elements.isEmpty();
        } else {
            for ( String segment : segments ) {
                if ( segment.length() > 0 ) {
                    return false;
                }
            }
        }
        return true;
    }

    public Set<CollectionElement> getElements() {
        if ( elements != null ) {
            return elements;
        } else {
            elements = new HashSet<CollectionElement>();
            for ( String segment : segments ) {
                if ( segment.length() > 0 ) {
                    for ( String element : Arrays.asList(segment.split("\n")) ) {
                        elements.add(CollectionElement.deserialize(element));
                    }
                }
            }
            return elements;
        }
    }

    public void clearElements() {
        elements = null;
        segments.clear();
    }

    public void setElements(Set<CollectionElement> elements) {
        this.elements = elements;
        segments.clear();
        StringBuilder newsegment = new StringBuilder();
        for ( CollectionElement element : elements ) {
            String serializedElement = element.serialize();
            if ( newsegment.length() >= (MAX_SEGMENT_SIZE+serializedElement.length()) ) {
                segments.add(newsegment.toString());
                newsegment = new StringBuilder();
            }
            if ( newsegment.length() > 0 ) {
                newsegment.append("\n");
            }
            newsegment.append(serializedElement);
        }
        if ( newsegment.length() > 0 ) {
            segments.add(newsegment.toString());
        }
    }

    public boolean addElement(CollectionElement element) {
        if ( !containsElement(element) ) {
            elements = null;
            String serializedElement = element.serialize();
            String freesegment = "";
            for ( String segment : segments ) {
                if ( segment.length() < (MAX_SEGMENT_SIZE+serializedElement.length()) ) {
                    freesegment = segment;
                    segments.remove(segment);
                    break;
                }
            }
            if ( freesegment.length() > 0 ) {
                freesegment += ("\n" + serializedElement);
            } else {
                freesegment += serializedElement;
            }
            segments.add(freesegment);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeElement(CollectionElement element) {
        if ( containsElement(element) ) {
            elements = null;
            String newsegment = "";
            for ( String segment : segments ) {
                if (segment.contains(element.serialize())) {
                    newsegment = segment;
                    segments.remove(segment);
                    break;
                }
            }
            newsegment = newsegment.replaceAll("(?m)^(" + Pattern.quote(element.serialize()) + ")\n?", "");
            if ( newsegment.endsWith("\n") ) {
                newsegment = newsegment.substring(0, newsegment.length()-1);
            }
            if ( newsegment.length() > 0 ) {
                segments.add(newsegment);
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean containsElement(CollectionElement element) {
        for ( String segment : segments ) {
            if (segment.contains(element.serialize())) {
                return true;
            }
        }
        return false;
    }

    public boolean containsElementName(String name) {
        for ( String segment : segments ) {
            if (segment.contains("\t" + name + "\t")) {
                return true;
            }
        }
        return false;
    }

    public boolean containsElementKey(String key) {
        for ( String segment : segments ) {
            if (segment.contains("\t" + key)) {
                return true;
            }
        }
        return false;
    }

    public CollectionElement findElementByName(String name) {
        Pattern pattern = Pattern.compile("(?s).*((" + Collection.OBJECT_TYPE + "|" + DataObject.OBJECT_TYPE + "|" + Link.OBJECT_TYPE + ")\t" + Pattern.quote(name) + "\t([0-9]{13})\t([0-9]+)\t([^\t]+)\t([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
        for ( String segment : segments ) {
            Matcher matcher = pattern.matcher(segment);
            if ( matcher.matches() ) {
                return CollectionElement.deserialize(matcher.group(1));
            }
        }
        return null;
    }

    @Override
    public MetadataElement findMetadataByName(String name) {
        Pattern pattern = Pattern.compile("(?s).*(" + Pattern.quote(name) + "/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})).*$");
        Matcher matcher = pattern.matcher(getMetadatasContent());
        if ( matcher.matches() ) {
            return MetadataElement.deserialize(matcher.group(1));
        }
        return null;
    }

    @Override
    public OrtolangObjectIdentifier getObjectIdentifier() {
        return new OrtolangObjectIdentifier(CoreService.SERVICE_NAME, Collection.OBJECT_TYPE, getId());
    }

}
