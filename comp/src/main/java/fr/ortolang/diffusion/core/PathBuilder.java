package fr.ortolang.diffusion.core;

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


public class PathBuilder {

    public static final String PATH_SEPARATOR = "/";
    public static final char PATH_SEPARATOR_CHAR = '/';
    //private static final int MAX_PATH_SIZE = 4096;
    private static final int MAX_PATHPART_SIZE = 255;
    private static final String VALID_PATH_REGEXP = PATH_SEPARATOR + "|" + PATH_SEPARATOR + "[^\\/?%*:|\"<>~\t]{1," + MAX_PATHPART_SIZE + "}+(" + PATH_SEPARATOR + "[^\\/?%*:|\"<>~\t]{1," + MAX_PATHPART_SIZE + "}+)*";

    private StringBuilder builder;

    private PathBuilder() {
        builder = new StringBuilder();
    }

    private PathBuilder(String path) throws InvalidPathException {
        this();
        this.path(path);
    }

    public static PathBuilder newInstance() {
        return new PathBuilder();
    }

    public static PathBuilder fromPath(String path) throws InvalidPathException {
        return new PathBuilder(path);
    }

    public PathBuilder path(String path) throws InvalidPathException {
        String part = normalize(path);
        if ( !part.equals(PATH_SEPARATOR) ) {
            builder.append(part);
        }
        return this;
    }

    public PathBuilder path(PathBuilder path) throws InvalidPathException {
        builder = builder.append(path.builder);
        return this;
    }

    public String build() {
        if ( builder.length() <= 1 ) {
            return PATH_SEPARATOR;
        }
        return builder.toString();
    }

    public String[] buildParts() {
        if ( builder.length() <= 1 ) {
            return new String[0];
        }
        return builder.substring(1).split(PATH_SEPARATOR);
    }

    public boolean isRoot() {
        return builder.length() <= 1;
    }

    public int depth() {
        if ( builder.length() <= 1 ) {
            return 0;
        }
        String path = builder.toString();
        return path.length() - path.replace(PATH_SEPARATOR, "").length();
    }

    public PathBuilder parent() {
        if ( builder.length() >= 1 ) {
            builder.delete(builder.lastIndexOf(PATH_SEPARATOR), builder.length());
        }
        if ( builder.length() == 0 ) {
            builder.append(PATH_SEPARATOR);
        }
        return this;
    }

    public PathBuilder relativize(String path) throws InvalidPathException {
        String parent = normalize(path);
        if (checkFiliation(parent, builder.toString())) {
            builder = builder.delete(0, parent.length());
        } else {
            throw new InvalidPathException("path " + path + " is not parent");
        }
        return this;
    }

    public PathBuilder relativize(int level) throws InvalidPathException {
        if ( level <= 0 ) {
            throw new InvalidPathException("level cannot be less or equal to 0");
        }
        if ( this.depth() < level ) {
            throw new InvalidPathException("path does not contains enough level to relativize");
        }
        if ( this.depth() == level ) {
            return new PathBuilder();
        }
        for ( int i=0; i<level; i++) {
            builder = builder.delete(0, builder.indexOf(PATH_SEPARATOR)+1);
            builder = builder.delete(0, builder.indexOf(PATH_SEPARATOR));
        }
        return this;
    }

    public String part() {
        if ( builder.length() <= 1 ) {
            return "";
        }
        return builder.substring(builder.lastIndexOf(PATH_SEPARATOR)+1);
    }

    public boolean isChild(String path) throws InvalidPathException {
        return checkFiliation(normalize(path), builder.toString());
    }

    public boolean isChild(PathBuilder path) throws InvalidPathException {
        return checkFiliation(path.build(), builder.toString());
    }

    public boolean isParent(String path) throws InvalidPathException {
        return checkFiliation(builder.toString(), normalize(path));
    }

    public boolean isParent(PathBuilder path) throws InvalidPathException {
        return checkFiliation(builder.toString(), path.build());
    }

    private String normalize(String path) throws InvalidPathException {
        String[] paths = path.split(PATH_SEPARATOR);
        String[] newPaths = new String[paths.length];
        StringBuilder newPath = new StringBuilder();
        int index = 0;

        for (String pathPart : paths) {
            if (!pathPart.isEmpty() && !".".equals(pathPart)) {
                if ("..".equals(pathPart)) {
                    if (index > 0) {
                        index--;
                    }
                } else {
                    newPaths[index] = pathPart;
                    index++;
                }
            }
        }
        for (int i = 0; i < index; i++) {
            newPath.append(PATH_SEPARATOR).append(newPaths[i]);
        }
        //root case
        if (newPath.length() == 0) {
            newPath.append(PATH_SEPARATOR);
        }
        String npath = newPath.toString();
        valid(npath);
        return npath;
    }

    private void valid(String path) throws InvalidPathException {
        if (!path.matches(VALID_PATH_REGEXP)) {
            throw new InvalidPathException(path);
        }
    }

    private boolean checkFiliation(String parent, String children) {
        if (parent.length() <= 1) {
            return true;
        }
        if (children.length() <= parent.length()) {
            return false;
        }
        return children.startsWith(parent) && children.charAt(parent.length()) == PATH_SEPARATOR_CHAR;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathBuilder other = (PathBuilder) obj;
        return builder.toString().equals(other.builder.toString());
    }

    @Override
    public int hashCode() {
        return builder != null ? builder.hashCode() : 0;
    }

    @Override
    public PathBuilder clone() {
        PathBuilder clone = new PathBuilder();
        clone.builder = new StringBuilder().append(builder);
        return clone;
    }

}