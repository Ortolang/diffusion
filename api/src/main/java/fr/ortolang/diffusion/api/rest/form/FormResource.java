package fr.ortolang.diffusion.api.rest.form;

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

import fr.ortolang.diffusion.api.rest.object.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.rest.template.Template;
import fr.ortolang.diffusion.form.FormService;
import fr.ortolang.diffusion.form.FormServiceException;
import fr.ortolang.diffusion.form.entity.Form;
import fr.ortolang.diffusion.registry.KeyNotFoundException;

@Path("/forms")
@Produces({ MediaType.APPLICATION_JSON })
public class FormResource {

	private Logger logger = Logger.getLogger(FormResource.class.getName());

	@EJB
	private FormService form;
	@Context
	private UriInfo uriInfo;
	
	public FormResource() {
	}
	
	@GET
	@Template( template="forms/list.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response list() throws FormServiceException {
		logger.log(Level.INFO, "list availables forms");
		
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
	@Template( template="forms/detail.vm", types={MediaType.TEXT_HTML})
	@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
	public Response get(@PathParam(value = "key") String key) throws FormServiceException, KeyNotFoundException {
		logger.log(Level.INFO, "read form for key: " + key);
				
		Form representation = form.readForm(key);
		
		return Response.ok(representation).build();
	}

}
