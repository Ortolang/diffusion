package fr.ortolang.diffusion;

import fr.ortolang.diffusion.indexing.NotIndexableContentException;
import fr.ortolang.diffusion.registry.KeyNotFoundException;
import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.registry.RegistryServiceException;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;

public class OrtolangIndexableObjectFactory<T> {

    public static OrtolangIndexableObject<IndexablePlainTextContent> buildPlainTextIndexableObject(String key) throws OrtolangException {
        try {
            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
            IndexablePlainTextContent content = service.getIndexablePlainTextContent(key);
            OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
            loadCommonIndexableObject(key, identifier, object);
            object.setContent(content);
            return object;
        } catch (Exception e) {
            throw new OrtolangException("unable to get plain text indexable content for object ", e);
        }
    }

    public static OrtolangIndexableObject<IndexableJsonContent> buildJsonIndexableObject(String key) throws OrtolangException, NotIndexableContentException {
        try {
            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
            OrtolangObjectIdentifier identifier = registry.lookup(key);
            OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
            IndexableJsonContent content = service.getIndexableJsonContent(key);
            OrtolangIndexableObject<IndexableJsonContent> object = new OrtolangIndexableObject<IndexableJsonContent>();
            loadCommonIndexableObject(key, identifier, object);
            object.setContent(content);
            return object;
        } catch (RegistryServiceException | KeyNotFoundException e) {
            throw new OrtolangException("unable to get json indexable content for object ", e);
        }
    }

    private static void loadCommonIndexableObject(String key, OrtolangObjectIdentifier identifier, OrtolangIndexableObject<?> object) throws OrtolangException {
        try {
            RegistryService registry = (RegistryService)OrtolangServiceLocator.lookup(RegistryService.SERVICE_NAME);
            object.setKey(key);
            object.setIdentifier(identifier);
            object.setService(identifier.getService());
            object.setType(identifier.getType());
            object.setHidden(registry.isHidden(key));
            object.setStatus(registry.getPublicationStatus(key));
            object.setProperties(registry.getProperties(key));
            object.setAuthor(registry.getAuthor(key));
            object.setCreationDate(registry.getCreationDate(key));
            object.setLastModificationDate(registry.getLastModificationDate(key));
            object.setName(key);
        } catch (Exception e) {
            throw new OrtolangException("unable to get json indexable content for object ", e);
        }
    }

}
