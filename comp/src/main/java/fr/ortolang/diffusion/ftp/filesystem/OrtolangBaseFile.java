package fr.ortolang.diffusion.ftp.filesystem;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.ftpserver.ftplet.FtpFile;

import fr.ortolang.diffusion.core.PathBuilder;

public class OrtolangBaseFile implements FtpFile {

    private PathBuilder path;
    private String name;
    private boolean hidden;
    private boolean directory;
    private boolean exists;
    private boolean readable;
    private boolean writeable;
    private boolean removable;
    private String owner;
    private String group;
    private long lastModified;
    private long size;

    public OrtolangBaseFile(PathBuilder path, String name) {
        this(path, name, true, false);
    }

    public OrtolangBaseFile(PathBuilder path, String name,  boolean directory, boolean exists) {
        this(path, name, directory, exists, false, false, 0, 0);
    }

    public OrtolangBaseFile(PathBuilder path, String name, boolean directory, boolean exists, boolean writeable, boolean removable) {
        this(path, name, directory, exists, writeable, removable, 0, 0);
    }

    public OrtolangBaseFile(PathBuilder path, String name, boolean directory, boolean exists, boolean writeable, boolean removable, long lastModified, long size) {
        this.path = path;
        this.name = name;
        this.hidden = false;
        this.directory = directory;
        this.exists = exists;
        this.readable = true;
        this.writeable = writeable;
        this.removable = removable;
        this.lastModified = lastModified;
        this.size = size;
        this.owner = "user";
        this.group = "group";
        this.lastModified = System.currentTimeMillis();
    }

    public PathBuilder getPath() {
        return path;
    }

    public void setPath(PathBuilder path) {
        this.path = path;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @Override
    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    @Override
    public boolean doesExist() {
        return exists;
    }

    public void setExist(boolean exists) {
        this.exists = exists;
    }

    @Override
    public boolean isReadable() {
        return readable;
    }

    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    public boolean isWriteable() {
        return writeable;
    }

    public void setWriteable(boolean writeable) {
        this.writeable = writeable;
    }

    @Override
    public boolean isRemovable() {
        return removable;
    }

    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public boolean setLastModified(long lastModified) {
        this.lastModified = lastModified;
        return true;
    }

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public boolean delete() {
        return false;
    }


    @Override
    public String getAbsolutePath() {
        return path.build();
    }

    @Override
    public String getGroupName() {
        return group;
    }

    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public String getOwnerName() {
        return owner;
    }

    @Override
    public boolean isFile() {
        return !directory;
    }

    @Override
    public boolean isWritable() {
        return writeable;
    }

    @Override
    public List<FtpFile> listFiles() {
        return Collections.emptyList();
    }

    @Override
    public boolean mkdir() {
        return false;
    }

    @Override
    public boolean move(FtpFile dest) {
        return false;
    }

    @Override
    public InputStream createInputStream(long arg0) throws IOException {
        throw new IOException("unable to create input stream for base file");
    }

    @Override
    public OutputStream createOutputStream(long arg0) throws IOException {
        throw new IOException("unable to create input stream for base file");
    }

}
