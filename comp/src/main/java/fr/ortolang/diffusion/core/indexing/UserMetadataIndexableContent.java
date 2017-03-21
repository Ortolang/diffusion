package fr.ortolang.diffusion.core.indexing;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangObjectIdentifier;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.core.entity.MetadataFormat;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.core.entity.Workspace;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class UserMetadataIndexableContent extends OrtolangObjectIndexableContent {

    private static final Logger LOGGER = Logger.getLogger(UserMetadataIndexableContent.class.getName());

    public static final String INDEX = "metadata";
    
    public UserMetadataIndexableContent(MetadataObject metadata, MetadataFormat format) throws IndexingServiceException, OrtolangException, KeyNotFoundException, RegistryServiceException {
        super(metadata, INDEX, format.getName());
        RegistryService registry = (RegistryService) OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME, RegistryService.class);
        OrtolangObjectIdentifier objectIdentifier = registry.lookup(metadata.getTarget());
        content.put("target", metadata.getTarget());
        content.put("targetType", objectIdentifier.getType());
        content.put("size", metadata.getSize());
        content.put("mimeType", metadata.getContentType());
        if (format != null && format.isIndexable() && metadata.getStream() != null && metadata.getStream().length() > 0) {
            BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
            ObjectMapper mapper = new ObjectMapper();
            try {
                content.put("streamContent",  mapper.readValue(binary.getFile(metadata.getStream()), Map.class));
            } catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
        }
        setContent(content);
    }

}
