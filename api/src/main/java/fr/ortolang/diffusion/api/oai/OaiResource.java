package fr.ortolang.diffusion.api.oai;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dom4j.DocumentException;
import org.jboss.resteasy.annotations.GZIP;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.GenericCollectionRepresentation;
import fr.ortolang.diffusion.api.oai.metadata.GenericOaiXmlParser;
import fr.ortolang.diffusion.api.oai.metadata.OaiXmlDocument;
import fr.ortolang.diffusion.api.oai.metadata.cmdi.CmdiDocument;
import fr.ortolang.diffusion.api.oai.metadata.cmdi.CmdiParser;
import fr.ortolang.diffusion.oai.OaiService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.exception.RecordNotFoundException;
import fr.ortolang.diffusion.template.TemplateEngine;
import fr.ortolang.diffusion.template.TemplateEngineException;

@Path("/oai")
public class OaiResource {

    private static final Logger LOGGER = Logger.getLogger(OaiResource.class.getName());

    private static final ClassLoader TEMPLATE_ENGINE_CL = OaiResource.class.getClassLoader();

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
    @GZIP
    public Response findRecord(@PathParam(value = "id") String id, @PathParam(value = "metadataPrefix") String metadataPrefix, @DefaultValue("*") @HeaderParam("accept") String accept) throws RecordNotFoundException {
        Record myRecord = oai.findRecord(id, metadataPrefix);
        String contentValue = "";
        String contentType = MediaType.TEXT_HTML;

        if ( accept.equals("application/x-cmdi+xml") || accept.equals("application/xml") ) {
            contentValue = myRecord.getXml();
            contentType = MediaType.APPLICATION_XML;
        } else {

            try {
                OaiXmlDocument document = null;
                if ( metadataPrefix.equals("cmdi") ) {
                    document = CmdiParser.newInstance().parse(myRecord.getXml()).getDoc();
                } else {
                    document = GenericOaiXmlParser.newInstance().parse(myRecord.getXml()).getDoc();
                }

                OaiRecordRepresentation representation = new OaiRecordRepresentation(
                    OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_CONTEXT),
                    RecordRepresentation.fromRecord(myRecord),
                    document
                );

                contentValue = TemplateEngine.getInstance(TEMPLATE_ENGINE_CL).process(metadataPrefix, representation);
            } catch (TemplateEngineException | DocumentException e) {
                LOGGER.log(Level.SEVERE, "Unable to generate HTML from OAI record template", e);
            }
        }

    	return Response.ok(contentValue).header("content-type", contentType).build();
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
