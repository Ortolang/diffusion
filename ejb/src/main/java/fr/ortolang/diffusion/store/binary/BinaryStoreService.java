package fr.ortolang.diffusion.store.binary;

import java.io.InputStream;

/**
 * <p>
 * <b>Binary Store</b> for ORTOLANG Diffusion Server.<br/>
 * This binary store is an internal service dedicated to object's binary content storage and retrieve. 
 * It stores binaries object's content in an optimal way in order to avoid duplication. The service generate identifiers 
 * based on binaries object's content (hash) using a BinaryHashGenerator. In this way, the same binary content cannot 
 * be stored two times.  
 * </p>
 * <p>
 * This binary storage service should meet those requirements :
 * <ul>
 * <li>store and retrieve binary object's content</li>
 * <li>avoid duplication of objects</li>
 * <li>use object's content based identifiers (hash)</li>
 * <li>provide the fastest store and retrieve operations</li>
 * <li>handle billions of objects</li>
 * </ul>
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface BinaryStoreService {
	
	public static final String SERVICE_NAME = "binary-store";

	/**
	 * Retrieve the data associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return an InputStream of the data
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	InputStream get(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the data size associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a long representing the size
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	long size(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the data mime type associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a String representing the mime type
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	String type(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Extract the plain texte part of the data associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a String representing the plain text extraction
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	String extract(String hash) throws BinaryStoreServiceException, DataNotFoundException;

	/**
	 * Insert some data in the storage.<br/>
	 * The storage will generate a hash for this data and insert it using this hash as identifier in the storage.<br/>
	 * If the data already exists this method does nothing
	 * 
	 * @param data
	 *            The data to store
	 * @return a String representation of the generated hash
	 * @throws DataCollisionException
	 *             if the hash already exists in the storage but for a different data
	 */
	String put(InputStream data) throws BinaryStoreServiceException, DataCollisionException;

	/**
	 * Check that the data is not corrupted, meaning that the data has not changed since it has been stored.
	 * 
	 * @param hash
	 *            The hash of the object to verify
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 * @throws DataCorruptedException
	 *             if the data has been corrupted
	 */
	void check(String hash) throws BinaryStoreServiceException, DataNotFoundException, DataCorruptedException;

	/**
	 * Generate a hash for this data. This operation MUST generate the same hash for the
	 * same data along time and instances.
	 * 
	 * @param data
	 *            The data on which to generate a hash
	 * @return a String representation of the generated hash
	 */
	String generate(InputStream data) throws BinaryStoreServiceException;

	/**
	 * Remove data from the storage.
	 * 
	 * @param hash
	 *            The hash of the data to remove
	 */
	void delete(String hash) throws BinaryStoreServiceException, DataNotFoundException;

}
