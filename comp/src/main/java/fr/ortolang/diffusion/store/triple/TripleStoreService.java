package fr.ortolang.diffusion.store.triple;

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

import java.io.OutputStream;

import org.openrdf.query.QueryLanguage;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public interface TripleStoreService {
	
	public static final String SERVICE_NAME = "triple-store";

	public static final String SERQL_QUERY_LANGUAGE = QueryLanguage.SERQL.getName();
    public static final String SPARQL_QUERY_LANGUAGE = QueryLanguage.SPARQL.getName();
    
    public static final String BASE_CONTEXT_URI = "http://www.ortolang.fr/rdf-context#";

	public void importOntology(String ontologyURI, String resourceName) throws TripleStoreServiceException;

	public void index(OrtolangIndexableObject object) throws TripleStoreServiceException;
	
	public void reindex(OrtolangIndexableObject object) throws TripleStoreServiceException;
	
	public void remove(String key) throws TripleStoreServiceException;
	
	public String query(String language, String query, String languageResult) throws TripleStoreServiceException;
	
	public void query(String language, String query, OutputStream os, String languageResult) throws TripleStoreServiceException;

}
