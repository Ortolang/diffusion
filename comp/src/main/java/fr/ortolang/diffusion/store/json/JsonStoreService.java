package fr.ortolang.diffusion.store.json;

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

import java.io.InputStream;
import java.util.List;

import com.orientechnologies.orient.core.record.impl.ODocument;
import fr.ortolang.diffusion.OrtolangIndexableObject;
import fr.ortolang.diffusion.OrtolangService;

public interface JsonStoreService extends OrtolangService {

	String SERVICE_NAME = "json-store";
	
	String INFO_PATH = "path";
    String INFO_SIZE = "size";
    String INFO_FILES = "files";
    String INFO_MAX_PARTITION_SIZE = "pool.maxpartitionsize";
    String INFO_AVAIL_CONNECTIONS = "connections.avail";
    String INFO_INSTANCES_CREATED = "instances.created";
    String INFO_DB_NAME = "db.name";
    String INFO_DB_SIZE = "db.size";
    String INFO_DB_STATUS = "db.status";
       
	
	void index(OrtolangIndexableObject<IndexableJsonContent> object) throws JsonStoreServiceException;
	
	void remove(String key) throws JsonStoreServiceException;
	
	List<String> search(String query) throws JsonStoreServiceException;

    List<ODocument> systemSearch(String query) throws JsonStoreServiceException;

    void systemInsertDocument(String type, InputStream document) throws JsonStoreServiceException;
    
    void systemInsertDocument(String type, String document) throws JsonStoreServiceException;
    
    String systemGetDocument(String key) throws JsonStoreServiceException;
    
}
