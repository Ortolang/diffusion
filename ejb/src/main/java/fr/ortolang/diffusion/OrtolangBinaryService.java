package fr.ortolang.diffusion;

import java.util.List;

/**
 * <p>
 * A BinaryDataSource is a service that store binary data for its objects. As binary data are all stored in the 
 * BinaryStore using hash top avoid duplication, such services should provide the list of their DigitalObject 
 * that are pointing to a specific hash. Using this interface, the platform is able to discover the usage of a binary 
 * data in all the DigitalObjects registered.
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface OrtolangBinaryService {
	
	public abstract List<OrtolangObject> findObjectByBinaryHash(String hash) throws OrtolangException;

}
