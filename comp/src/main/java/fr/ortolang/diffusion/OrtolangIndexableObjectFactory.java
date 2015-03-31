package fr.ortolang.diffusion;

import fr.ortolang.diffusion.registry.RegistryService;
import fr.ortolang.diffusion.store.index.IndexablePlainTextContent;
import fr.ortolang.diffusion.store.json.IndexableJsonContent;
import fr.ortolang.diffusion.store.triple.IndexableSemanticContent;

public class OrtolangIndexableObjectFactory<T> {
	
	public static OrtolangIndexableObject<IndexablePlainTextContent> buildPlainTextIndexableObject(String key) throws OrtolangException {
		try {
			RegistryService registry = (RegistryService)OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
			IndexablePlainTextContent content = service.getIndexablePlainTextContent(key);
			OrtolangIndexableObject<IndexablePlainTextContent> object = new OrtolangIndexableObject<IndexablePlainTextContent>();
			loadBasicIndexableObject(key, identifier, object);
			object.setContent(content);
			return object;
		} catch (Exception e) {
			throw new OrtolangException("unable to get plain text indexable content for object ", e);
		}
	}
	
	public static OrtolangIndexableObject<IndexableJsonContent> buildJsonIndexableObject(String key) throws OrtolangException {
		try {
			RegistryService registry = (RegistryService)OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
			IndexableJsonContent content = service.getIndexableJsonContent(key);
			OrtolangIndexableObject<IndexableJsonContent> object = new OrtolangIndexableObject<IndexableJsonContent>();
			loadBasicIndexableObject(key, identifier, object);
			object.setContent(content);
			return object;
		} catch (Exception e) {
			throw new OrtolangException("unable to get json indexable content for object ", e);
		}
	}
	
	public static OrtolangIndexableObject<IndexableSemanticContent> buildSemanticIndexableObject(String key) throws OrtolangException {
		try {
			RegistryService registry = (RegistryService)OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
			OrtolangObjectIdentifier identifier = registry.lookup(key);
			OrtolangIndexableService service = OrtolangServiceLocator.findIndexableService(identifier.getService());
			IndexableSemanticContent content = service.getIndexableSemanticContent(key);
			OrtolangIndexableObject<IndexableSemanticContent> object = new OrtolangIndexableObject<IndexableSemanticContent>();
			loadBasicIndexableObject(key, identifier, object);
			object.setContent(content);
			return object;
		} catch (Exception e) {
			throw new OrtolangException("unable to get semantic indexable content for object ", e);
		}
	}
	
	private static void loadBasicIndexableObject(String key, OrtolangObjectIdentifier identifier, OrtolangIndexableObject<?> object) throws OrtolangException {
		try {
			RegistryService registry = (RegistryService)OrtolangServiceLocator.findService(RegistryService.SERVICE_NAME);
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
