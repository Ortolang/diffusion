package fr.ortolang.diffusion.storage;

import java.io.InputStream;

/**
 * <p>
 * <b>Storage Service</b> for ORTOLANG Diffusion Server.<br/>
 * This storage service is an internal service dedicated to object storage and retrieve. It stores object's content
 * in an optimal way in order to avoid duplication. The service generate identifiers based on object's content using a 
 * hash function. In this way, the same content cannot be stored two times.  
 * </p>
 * <p>
 * This storage service should meet those requirements :
 * <ul>
 * <li>store and retrieve object's content</li>
 * <li>avoid duplication of objects</li>
 * <li>use universal object's identifiers based on object content (hash)</li>
 * <li>provide the fastest store and retrieve operations</li>
 * <li>handle billions of objects</li>
 * </ul>
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface StorageService {

	/**
	 * Retrieve an InputStream from the storage on the object associated with this identifier.<br/>
	 * 
	 * @param identifier
	 *            The identifier of the object
	 * @return an InputStream on the object
	 * @throws ObjectNotFoundException
	 *             if the identifier does not exists in the storage
	 */
	public InputStream get(String identifier) throws StorageServiceException, ObjectNotFoundException;

	/**
	 * Insert an object in the storage.<br/>
	 * The storage will generate an identifier for this object's content and insert this content using this identifier in the storage.<br/>
	 * 
	 * @param content
	 *            The object's content to store
	 * @return a String representation of the generated identifier
	 * @throws ObjectAlreadyExistsException
	 *             if the identifier already exists in the storage and the content is the same
	 * @throws ObjectCollisionException
	 *             if the identifier already exists in the storage and the content is NOT the same
	 */
	public String put(InputStream content) throws StorageServiceException, ObjectAlreadyExistsException, ObjectCollisionException;

	/**
	 * Check that the content is not corrupted, meaning that the object's content has not changed since it has been stored.
	 * 
	 * @param identifier
	 *            The identifier of the object to verify
	 * @throws ObjectNotFoundException
	 *             if the identifier does not exists in the storage
	 * @throws ObjectCorruptedException
	 *             if the content has been corrupted
	 */
	public void check(String identifier) throws StorageServiceException, ObjectNotFoundException, ObjectCorruptedException;

	/**
	 * Generate an identifier for this object's content. This operation must be idempotent and generate the same identifier for the
	 * same object's content along time and instances.
	 * 
	 * @param content
	 *            The object's content to generate an identifier for.
	 * @return a String representation of the generated identifier
	 */
	public String generate(InputStream content) throws StorageServiceException;

	/**
	 * Remove an object from the storage.
	 * 
	 * @param identifier
	 *            The identifier of the object to delete
	 */
	public void delete(String identifier) throws StorageServiceException, ObjectNotFoundException;

}
