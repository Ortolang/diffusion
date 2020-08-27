package fr.ortolang.diffusion.oai.format;

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

import javax.json.JsonObject;
import javax.json.JsonString;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.store.handle.HandleStoreService;

public class OAI_DC extends DCXMLDocument {

	public OAI_DC() {
		header = "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/elements/1.1/ http://dublincore.org/schemas/xmls/qdc/2006/01/06/dc.xsd\">";
		footer = "</oai_dc:dc>";
	}

	public static String identifier(String wsalias) {
		return identifier(wsalias, null);
	}

	public static String identifier(String wsalias, String snapshotName) {
		return HandleStoreService.HDL_PROXY_URL
				+ OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.HANDLE_PREFIX) + "/" + wsalias
				+ ((snapshotName != null) ? "/" + snapshotName : "");
	}

	public static String person(JsonObject contributor) {
		JsonObject entityContributor = contributor.getJsonObject("entity");
		JsonString lastname = entityContributor.getJsonString("lastname");
		JsonString midname = entityContributor.getJsonString("midname");
		JsonString firstname = entityContributor.getJsonString("firstname");
		JsonString title = entityContributor.getJsonString("title");
		JsonString acronym = null;

		if (entityContributor.containsKey("organization")) {
			acronym = entityContributor.getJsonObject("organization").getJsonString("acronym");
		}
		return (lastname != null ? lastname.getString() : "") + (midname != null ? ", " + midname.getString() : "")
				+ (firstname != null ? ", " + firstname.getString() : "")
				+ (title != null ? " " + title.getString() : "") + (acronym != null ? ", " + acronym.getString() : "");
	}
}
