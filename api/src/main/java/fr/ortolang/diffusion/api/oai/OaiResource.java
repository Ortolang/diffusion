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
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;

@Path("/oai")
public class OaiResource {

    @EJB
    private OaiService oai;
    
    public OaiResource() {
    }
    
    @GET
    @Path("/records")
    @Produces({ MediaType.APPLICATION_JSON })
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
    @Path("/records/{id}/{metadataPrefix}")
    @Produces({ MediaType.APPLICATION_XML })
    @GZIP
    public Response findRecord(@PathParam(value = "id") String id, @PathParam(value = "metadataPrefix") String metadataPrefix) throws RecordNotFoundException {
        Record myRecord = oai.findRecord(id, metadataPrefix);
    	return Response.ok(myRecord.getXml()).build();
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
