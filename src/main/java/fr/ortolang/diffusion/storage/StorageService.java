package fr.ortolang.diffusion.storage;

/**
 * <p>
 * <b>Storage Service</b> for Ortlang Diffusion Server.<br/>
 * This storage service is an internal service dedicated to digital object storage and retrieve.
 * </p>
 * <p>
 * This storage service should meet those requirements : 
 * <ul>
 * <li>store and retrieve digital object's content</li>
 * <li>avoid duplication of objects</li>
 * <li>use universal identifiers for objects based on object content (hash)</li>
 * <li>provide the fastest store and retrieve operations</li>
 * <li>handle billions of objects</li>
 * <li>ensure ACID properties</li>
 * </ul>
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface StorageService {
	
	/**
	 * Retrieve a DigitalObject from the storage using its identifier.<br/>
	 * 
	 * @param identifier
	 *            The identifier of the DigitalObject
	 * @return the DigitalObject corresponding to this identifier
	 * @throws ObjectNotFoundException
	 *             if the identifier does not exists in the storage
	 * @see DigitalObject
	 */
	public DigitalObject get(String identifier) throws StorageServiceException, ObjectNotFoundException;

	/**
	 * Insert an DigitalObject in the storage.<br/>
	 * The storage will generate an identifier for this DigitalObject and insert the DigitalObject using this 
	 * identifier in the storage.<br/>
	 * 
	 * @param object
	 *            The DigitalObject to store
	 * @return a String representation of the generated identifier
	 * @throws ObjectAlreadyExistsException
	 *             if the identifier already exists in the storage and the DigitalObject is the same
	 * @throws ObjectCollisionException
	 *             if the identifier already exists in the storage and the DigitalObject is NOT the same
	 * @see DigitalObject
	 */
	public String put(DigitalObject object) throws StorageServiceException, ObjectAlreadyExistsException, ObjectCollisionException;

	/**
	 * Verify that the DigitalObject is not corrupted, meaning that the object content 
	 * has not changed since it has been stored. 
	 * 
	 * @param identifier
	 *            The hash identifier of the DigitalObject to verify
	 * @throws ObjectNotFoundException
	 *             if the identifier does not exists in the storage
	 * @throws ObjectCorruptedException
	 *             if the content has been corrupted
	 */
	public void verify(String identifier) throws StorageServiceException, ObjectNotFoundException, ObjectCorruptedException;

	/**
	 * Generate an identifier for a DigitalObject based on a hash function. This operation must be idempotent and generate 
	 * the same identifier for the same DigitalObject.
	 * 
	 * @param object
	 *            The DigitalObject to generate an identifier for.
	 * @return a String representation of the generated identifier
	 * @see DigitalObject
	 */
	public String hash(DigitalObject object) throws StorageServiceException;

	/**
	 * Remove an DigitalObject from the storage.
	 * 
	 * @param identifier
	 *            The identifier of the DigitalObject to delete
	 */
	public void delete(String identifier) throws StorageServiceException, ObjectNotFoundException;

}
