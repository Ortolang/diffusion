package fr.ortolang.diffusion.storage.binary;

import java.io.InputStream;

import fr.ortolang.diffusion.storage.ObjectAlreadyExistsException;
import fr.ortolang.diffusion.storage.ObjectCollisionException;
import fr.ortolang.diffusion.storage.ObjectCorruptedException;
import fr.ortolang.diffusion.storage.ObjectNotFoundException;

/**
 * <p>
 * <b>Binary Storage Service</b> for ORTOLANG Diffusion Server.<br/>
 * This binary storage service is an internal service dedicated to binaries objects storage and retrieve. 
 * It stores binaries object's content in an optimal way in order to avoid duplication. The service generate identifiers 
 * based on binaries object's content using an IdentifierGeneratorService. In this way, the same content cannot be stored 
 * two times.  
 * </p>
 * <p>
 * This binary storage service should meet those requirements :
 * <ul>
 * <li>store and retrieve binary object's content</li>
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
public interface BinaryStorageService {

	/**
	 * Retrieve an InputStream from the storage on the object associated with this identifier.<br/>
	 * 
	 * @param identifier
	 *            The identifier of the object
	 * @return an InputStream on the object
	 * @throws ObjectNotFoundException
	 *             if the identifier does not exists in the storage
	 */
	public InputStream get(String identifier) throws BinaryStorageException, ObjectNotFoundException;

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
	public String put(InputStream content) throws BinaryStorageException, ObjectAlreadyExistsException, ObjectCollisionException;

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
	public void check(String identifier) throws BinaryStorageException, ObjectNotFoundException, ObjectCorruptedException;

	/**
	 * Generate an identifier for this object's content. This operation must be idempotent and generate the same identifier for the
	 * same object's content along time and instances.
	 * 
	 * @param content
	 *            The object's content to generate an identifier for.
	 * @return a String representation of the generated identifier
	 */
	public String generate(InputStream content) throws BinaryStorageException;

	/**
	 * Remove an object from the storage.
	 * 
	 * @param identifier
	 *            The identifier of the object to delete
	 */
	public void delete(String identifier) throws BinaryStorageException, ObjectNotFoundException;

}
