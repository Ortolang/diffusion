package fr.ortolang.diffusion.core.indexing;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.ortolang.diffusion.OrtolangException;
import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.core.OrtolangItemType;
import fr.ortolang.diffusion.core.entity.Collection;
import fr.ortolang.diffusion.core.entity.MetadataObject;
import fr.ortolang.diffusion.indexing.IndexingServiceException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;
import fr.ortolang.diffusion.store.binary.BinaryStoreServiceException;
import fr.ortolang.diffusion.store.binary.DataNotFoundException;

public class SuggestItemIndexableContent extends OrtolangIndexableContent {

    public static final String[] MAPPING;

    public static final String INDEX = "suggest";

    private static final Logger LOGGER = Logger.getLogger(SuggestItemIndexableContent.class.getName());

    private OrtolangItemType ortolangItemType;

    static {
        MAPPING = new String[] {
                "label_fr",
                "type=text",
                "path",
                "type=keyword",
                "type",
                "type=keyword"
        };
    }

    public SuggestItemIndexableContent(MetadataObject metadata, Collection collection, String alias) throws IndexingServiceException, OrtolangException {
        super();
        try {
            BinaryStoreService binary = (BinaryStoreService) OrtolangServiceLocator.lookup(BinaryStoreService.SERVICE_NAME, BinaryStoreService.class);
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> content = new HashMap<String, Object>();
            Map<String, Object> contentMetadata = mapper.readValue(binary.getFile(metadata.getStream()), new TypeReference<Map<String, Object>>(){});

            String metadataType = (String) contentMetadata.get(OrtolangItemType.METADATA_KEY);
//            ortolangItemType = OrtolangItemType.fromMetadataType(metadataType);
            setType(INDEX);
            
            setIndex(INDEX);
            setId(alias);
            content.put("id", alias);
            List<Map<String,String>> titleArray = extractTitle(contentMetadata);
            for(Map<String,String> titleObj : titleArray) {
            	content.put("label_"+titleObj.get("lang"),  titleObj.get("value"));
            }
            content.put("path", "content");
            content.put("type", metadataType);

            setContent(content);
            
            
        } catch (IOException | BinaryStoreServiceException | DataNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

	@SuppressWarnings("unchecked")
	private List<Map<String, String>> extractTitle(Map<String, Object> contentMetadata) {
		return (List<Map<String,String>>) contentMetadata.get("title");
	}
}
