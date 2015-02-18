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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;

import fr.ortolang.diffusion.OrtolangIndexableObject;

public class TripleStoreStatementBuilder {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	
	public static Set<Statement> buildStatements(OrtolangIndexableObject object) throws TripleStoreServiceException {
		Set<Statement> statements = new HashSet<Statement> ();
//		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://www.ortolang.fr/2014/05/diffusion#Object"));
//		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#service", object.getService()));
//		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#type", object.getType()));
//		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#author", URIHelper.fromKey(object.getAuthor())));
		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#creationDate", sdf.format(new Date(object.getCreationDate()))));
		statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#lastModificationDate", sdf.format(new Date(object.getLastModificationDate()))));
//		for ( OrtolangObjectProperty property : object.getProperties() ) {
//			statements.add(buildStatement(URIHelper.fromKey(object.getKey()), "http://www.ortolang.fr/2014/05/diffusion#" + property.getName(), property.getValue()));
//		}
		for ( Triple triple : object.getSemanticContent().getTriples() ) {
			statements.add(buildStatement(triple.getSubject(), triple.getPredicate(), triple.getObject()));
		}
		return statements;
	}
	
	private static Statement buildStatement(String subject, String predicate, String object) {
		URI usubject = new URIImpl(subject);
        URI upredicate = new URIImpl(predicate);
        Value uobject = null;
        try {
            uobject = new URIImpl(object);
        } catch (IllegalArgumentException iae) {
            uobject = new LiteralImpl(object);
        }
        return new StatementImpl(usubject, upredicate, uobject);
	}

}
