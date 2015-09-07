package fr.ortolang.diffusion.store.binary;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.File;
import java.io.InputStream;

import fr.ortolang.diffusion.OrtolangService;

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
public interface BinaryStoreService extends OrtolangService {
	
	public static final String SERVICE_NAME = "binary-store";

	/**
	 * Check that this hash exists in the store.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return true or false
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public boolean contains(String hash) throws BinaryStoreServiceException;
	
	/**
	 * Retrieve the data associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return an InputStream of the data
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public InputStream get(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the file object of the data associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return the File object
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public File getFile(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the data size associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a long representing the size
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public long size(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the data mime type associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a String representing the mime type
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public String type(String hash) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Retrieve the data mime type associated with this identifier and a filename for better resolution.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @param filename
	 *            The filename corresponding to that hash
	 * @return a String representing the mime type
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public String type(String hash, String filename) throws BinaryStoreServiceException, DataNotFoundException;
	
	/**
	 * Extract the plain text part of the data associated with this identifier.<br/>
	 * 
	 * @param hash
	 *            The hash of the data
	 * @return a String representing the plain text extraction
	 * @throws DataNotFoundException
	 *             if the hash does not exists in the storage
	 */
	public String extract(String hash) throws BinaryStoreServiceException, DataNotFoundException;

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
	public String put(InputStream data) throws BinaryStoreServiceException, DataCollisionException;
	
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
	public void check(String hash) throws BinaryStoreServiceException, DataNotFoundException, DataCorruptedException;

	/**
	 * Generate a hash for this data. This operation MUST generate the same hash for the
	 * same data along time and instances.
	 * 
	 * @param data
	 *            The data on which to generate a hash
	 * @return a String representation of the generated hash
	 */
	public String generate(InputStream data) throws BinaryStoreServiceException;

	/**
	 * Remove data from the storage.
	 * 
	 * @param hash
	 *            The hash of the data to remove
	 */
	public void delete(String hash) throws BinaryStoreServiceException, DataNotFoundException;

}
