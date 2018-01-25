package fr.ortolang.diffusion.api.oai;

import java.util.List;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.oai.OaiService;
import fr.ortolang.diffusion.oai.entity.Record;

@Path("/oai")
@Produces({ MediaType.APPLICATION_JSON })
public class OaiResource {

    @EJB
    private OaiService oai;
    
    public OaiResource() {
    }
    
    @GET
    @Path("/records")
    @GZIP
    public Response listRecords() {
    	List<Record> records = oai.listRecords();
    	long size = oai.countRecords();
    	GenericCollectionRepresentation<RecordRepresentation> representation = new GenericCollectionRepresentation<RecordRepresentation>();
    	for (Record rec : records) {
    		representation.addEntry(RecordRepresentation.fromRecord(rec));
    	}
    	representation.setSize(size);
    	return Response.ok(representation).build();
    }
    
    @GET
    @Path("/records/{id}")
    @GZIP
    public Response getRecord(@PathParam(value = "id") String id) {
    	return Response.ok().build();
    }
    
    @GET
    @Path("/sets")
    @GZIP
    public Response listSets() {
    	return Response.ok().build();
    }
    
    @GET
    @Path("/sets/{id}")
    @GZIP
    public Response getSet(@PathParam(value = "id") String id) {
    	return Response.ok().build();
    }
    
    
}
