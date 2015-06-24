package fr.ortolang.diffusion.api.form;

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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import fr.ortolang.diffusion.api.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.form.FormService;
import fr.ortolang.diffusion.form.FormServiceException;
import fr.ortolang.diffusion.form.entity.Form;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Path("/forms")
@Produces({ MediaType.APPLICATION_JSON })
public class FormResource {

	private static final Logger LOGGER = Logger.getLogger(FormResource.class.getName());

	@EJB
	private FormService form;
	@Context
	private UriInfo uriInfo;
	
	public FormResource() {
	}
	
	@GET
	public Response list() throws FormServiceException {
		LOGGER.log(Level.INFO, "GET /forms");
		
		List<Form> forms = form.listForms();
		
		GenericCollectionRepresentation<Form> representation = new GenericCollectionRepresentation<Form> ();
		for(Form form : forms) {
		    representation.addEntry(form);
		}
		representation.setOffset(0);
		representation.setSize(forms.size());
		representation.setLimit(forms.size());
		
		Response response = Response.ok(representation).build();
		return response;
	}
	
	@GET
	@Path("/{key}")
	public Response get(@PathParam(value = "key") String key) throws FormServiceException, KeyNotFoundException {
		LOGGER.log(Level.INFO, "GET /forms/" + key);
				
		Form representation = form.readForm(key);
		
		return Response.ok(representation).build();
	}

}