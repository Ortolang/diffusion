package fr.ortolang.diffusion.archive.facile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;
import org.jboss.ejb3.annotation.SecurityDomain;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.archive.exception.CheckArchivableException;
import fr.ortolang.diffusion.archive.facile.entity.Validator;

@Startup
@Local(FacileService.class)
@Singleton(name = FacileService.SERVICE_NAME)
@SecurityDomain("ortolang")
@PermitAll
public class FacileServiceBean implements FacileService {
    
    private static final Logger LOGGER = Logger.getLogger(FacileServiceBean.class.getName());

    private Client client;
    private WebTarget target;

    public FacileServiceBean() {
        // no need to initialize
    }

    public void setClient(Client cl) {
        this.client = cl;
    }

    @PostConstruct
    public void init() {
        if (OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FACILE_HOST) == null
        || OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FACILE_HOST)
        .length() == 0) {
            LOGGER.log(Level.INFO, "Facile not configured, skipping initialization");
            this.client = null;
            this.target = null;
        }
        
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String getServiceName() {
        return FacileService.SERVICE_NAME;
    }

    @Override
    public Map<String, String> getServiceInfos() {
        return Collections.emptyMap();
    }

    @Override
    public Validator checkArchivableFile(File content, String filename) throws CheckArchivableException {
        LOGGER.log(Level.FINE, "Checking archivable file with filename {}", filename);

        Validator validator = null;
        String xml = null;
        client = ClientBuilder.newClient();
        target = client.target(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FACILE_HOST))
                .path(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.FACILE_PATH));
        MultipartFormDataOutput mdo = new MultipartFormDataOutput();
        Response response = null;
        try (FileInputStream stream = new FileInputStream(content)) {
            mdo.addFormData("file", stream, MediaType.APPLICATION_OCTET_STREAM_TYPE, filename);
            GenericEntity<MultipartFormDataOutput> entity = new GenericEntity<MultipartFormDataOutput>(mdo) {
            };

            response = target.request(MediaType.MEDIA_TYPE_WILDCARD)
                    .post(Entity.entity(entity, MediaType.MULTIPART_FORM_DATA_TYPE));
            if (response.getStatus() != Status.OK.getStatusCode()) {
                response.close();
                throw new CheckArchivableException("unexpected response code: " + response.getStatus());
            }
            xml = response.readEntity(String.class);
        } catch (IOException e) {
            throw new CheckArchivableException("Unable to load file to Facile service", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        if (xml != null) {
        	try {
        		validator = Validator.fromXML(xml);
        	} catch (JAXBException e) {
        		throw new CheckArchivableException("Unable to load parse the xml file from Facile service", e);
        	}
        }
        return validator;
    }
}