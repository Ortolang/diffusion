package fr.ortolang.diffusion;

/**
 * <p>
 * Base interface for all services of the platform. A DiffusionService is responsible for exposing its name, the object types name it 
 * can handle and a generic find operation based on the global DiffusionObjectIdentifier
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface DiffusionService {

	public abstract String getServiceName();

	public abstract String[] getObjectTypeList();

	public abstract DiffusionObject findObject(DiffusionObjectIdentifier identifier) throws DiffusionServiceException;
	
}
