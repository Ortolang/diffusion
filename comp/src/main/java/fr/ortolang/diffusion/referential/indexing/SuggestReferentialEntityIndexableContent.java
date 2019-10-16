package fr.ortolang.diffusion.referential.indexing;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import fr.ortolang.diffusion.indexing.IndexableContentParsingException;
import fr.ortolang.diffusion.indexing.OrtolangIndexableContent;
import fr.ortolang.diffusion.referential.ReferentialService;
import fr.ortolang.diffusion.referential.entity.ReferentialEntity;
import fr.ortolang.diffusion.referential.entity.ReferentialEntityType;

public class SuggestReferentialEntityIndexableContent extends OrtolangIndexableContent {
	public static final String INDEX = "suggest";
    protected Map<String, Object> content;

    public SuggestReferentialEntityIndexableContent(ReferentialEntity entity) throws IndexableContentParsingException {
        super(INDEX, INDEX, entity.getObjectKey());
        content = new HashMap<>();
        
        // Copies referential to content
        JSONObject jsonObject = new JSONObject(entity.getContent());
        if (entity.getType().equals(ReferentialEntityType.ORGANIZATION)) {
        	content.put("label_fr", jsonObject.get("fullname"));
        	content.put("type", jsonObject.get("type"));
        	content.put("id", jsonObject.get("id"));
        	content.put("path", "producers.id[]");
        }
        content.put("key", entity.getKey());
        setContent(content);
    }
}
