package fr.ortolang.diffusion.store.handle;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;

import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ScanCallback;
import net.handle.hdllib.Util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.PreloadConfig;

public class ExportBDBHandles {

	private static Environment env;
	private static DBWrapper nasDB;
	private static DBWrapper handlesDB;

	@BeforeClass
	public static void setup() throws Exception {
		EnvironmentConfig envConfig = new EnvironmentConfig();
		envConfig.setTransactional(true);
		envConfig.setReadOnly(false);
		envConfig.setAllowCreate(true);
		envConfig.setSharedCache(true);
		env = new Environment(new File("/media/space/jerome/Data/SLDR/bdbje"), envConfig);
		nasDB = new DBWrapper(env, "nas");
		handlesDB = new DBWrapper(env, "handles");
	}

	@AfterClass
	public static void tearDown() throws DatabaseException, IOException {
		nasDB.close();
		handlesDB.close();
		env.close();
	}

	@Test
	public void export() throws HandleException {
	    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("/home/jerome/sldr_handles_new.json"))) {
	        writer.append("[");
    	    this.scanHandles(new ScanCallback() {
    			@Override
    			public void scanHandle(byte[] handle) throws HandleException {
    				byte[][] rawvalues = getRawHandleValues(handle, null, null);
    				for (int i=0; i<rawvalues.length; i++) {
    					HandleValue value = new HandleValue();
    					Encoder.decodeHandleValue(rawvalues[i], 0, value);
    					if ( value.getTypeAsString().equals("URL") ) {
    					    try {
                                writer.write("{\"handle\":\"" + Util.decodeString(handle) + "\",");
                                writer.write("\"url\":\"" + value.getDataAsString() + "\"}");
                                writer.write(",\r\n");
                            } catch (IOException e) {
                                System.err.println("Error during exporting handles: " + e.getMessage());
                            }
    					}
    				}
    			}
    		});
    	    writer.append("]");
    	    writer.flush();
	    } catch ( IOException e ) {
	        System.err.println("Error during exporting handles: " + e.getMessage());
	    }
	}

	private final void scanHandles(ScanCallback callback) throws HandleException {
		DBWrapper.DBIterator e = handlesDB.getEnumerator();
		try {
			while (e.hasMoreElements()) {
				byte[][] record = (byte[][]) e.nextElement();
				callback.scanHandle(record[0]);
			}
		} finally {
			e.cleanup();
		}
	}

	private final byte[][] getRawHandleValues(byte handle[], int indexList[], byte typeList[][]) throws HandleException {
		byte value[] = null;
		try {
			value = handlesDB.get(handle);
		} catch (Exception e) {
			HandleException he = new HandleException(HandleException.INTERNAL_ERROR, "Error retrieving handle");
			he.initCause(e);
			throw he;
		}
		if (value == null) {
			return null;
		}

		int clumpLen;
		int bufPos = 0;
		boolean allValues = (indexList == null || indexList.length == 0) && (typeList == null || typeList.length == 0);
		int numValues = Encoder.readInt(value, bufPos);
		bufPos += Encoder.INT_SIZE;
		int origBufPos = bufPos;

		// figure out the number of records matching this request...
		int matches = 0;
		byte clumpType[];
		int clumpIndex;
		if (allValues) {
			matches = numValues;
		} else {
			for (int i = 0; i < numValues; i++) {
				clumpLen = Encoder.readInt(value, bufPos);
				bufPos += Encoder.INT_SIZE;

				clumpType = Encoder.getHandleValueType(value, bufPos);
				clumpIndex = Encoder.getHandleValueIndex(value, bufPos);

				if (Util.isParentTypeInArray(typeList, clumpType) || Util.isInArray(indexList, clumpIndex))
					matches++;

				bufPos += clumpLen;
			}
		}

		// populate and return an array with the matched records...
		byte clumps[][] = new byte[matches][];
		int clumpNum = 0;
		bufPos = origBufPos;
		for (int i = 0; i < numValues; i++) {
			clumpLen = Encoder.readInt(value, bufPos);
			bufPos += Encoder.INT_SIZE;

			clumpType = Encoder.getHandleValueType(value, bufPos);
			clumpIndex = Encoder.getHandleValueIndex(value, bufPos);

			if (allValues || Util.isParentTypeInArray(typeList, clumpType) || Util.isInArray(indexList, clumpIndex)) {
				clumps[clumpNum] = new byte[clumpLen];
				System.arraycopy(value, bufPos, clumps[clumpNum], 0, clumpLen);
				clumpNum++;
			}

			bufPos += clumpLen;
		}
		return clumps;
	}

	private static class DBWrapper {
		private Database db;
		private String dbName;
		private Environment environment;

		public DBWrapper(Environment environment, String dbName) throws Exception {
			this.environment = environment;
			this.dbName = dbName;

			openDB();
		}

		private void openDB() throws Exception {
			com.sleepycat.je.Transaction openTxn = environment.beginTransaction(null, null);
			DatabaseConfig dbConfig = new DatabaseConfig();
			dbConfig.setTransactional(true);
			dbConfig.setAllowCreate(true);
			dbConfig.setSortedDuplicates(false);
			dbConfig.setReadOnly(environment.getConfig().getReadOnly());
			db = environment.openDatabase(openTxn, dbName, dbConfig);
			openTxn.commit();
			PreloadConfig preloadCfg = new PreloadConfig();
			preloadCfg.setMaxMillisecs(500);
			db.preload(preloadCfg);
		}

		public byte[] get(byte key[]) throws IOException, DatabaseException {
			DatabaseEntry dbVal = new DatabaseEntry();
			if (db.get(null, new DatabaseEntry(key), dbVal, null) == OperationStatus.SUCCESS) {
				return dbVal.getData();
			}
			return null;
		}

		@SuppressWarnings("unused")
		public void put(byte key[], byte data[]) throws IOException, Exception {
			OperationStatus status = db.put(null, new DatabaseEntry(key), new DatabaseEntry(data));
			if (status != OperationStatus.SUCCESS) {
				throw new Exception("Unknown status returned from db.put: " + status);
			}
		}

		public void close() throws IOException, DatabaseException {
			db.close();
		}

		@SuppressWarnings("unused")
		public boolean del(byte key[]) throws IOException, DatabaseException {
			return db.delete(null, new DatabaseEntry(key)) == OperationStatus.SUCCESS;
		}

		public DBIterator getEnumerator() {
			return new DBIterator();
		}

		@SuppressWarnings("unused")
		public DBIterator getEnumerator(byte filter[]) {
			return new DBIterator(filter);
		}

		@SuppressWarnings("unused")
		public void deleteAllRecords() throws Exception {
			Database tmpDB = db;
			com.sleepycat.je.Transaction killTxn = environment.beginTransaction(null, null);
			try {
				db = null;
				tmpDB.close();
				tmpDB = null;
				environment.truncateDatabase(killTxn, dbName, false);
				killTxn.commitSync();
			} catch (Exception t) {
				try {
					killTxn.abort();
				} catch (Throwable t2) {
				}
				throw t;
			} finally {
				if (db == null) // try to re-open the database, if necessary
					openDB();
			}
		}

		@SuppressWarnings("rawtypes")
		class DBIterator implements Enumeration {
			private Cursor cursor = null;
			private DatabaseEntry keyEntry = new DatabaseEntry();
			private DatabaseEntry valEntry = new DatabaseEntry();
			private OperationStatus lastStatus = null;
			byte currentValue[][] = null;
			byte prefix[] = null;

			public DBIterator() {
				try {
					cursor = db.openCursor(null, null);
					lastStatus = cursor.getFirst(keyEntry, valEntry, null);
					// System.err.println("opening iterator: status="+lastStatus+"; key="+keyEntry);

					if (lastStatus != null && lastStatus == OperationStatus.SUCCESS) {
						loadValueFromEntries();
					} else {
						cursor.close();
					}

				} catch (Exception e) {
					System.err.println("Error in DBIterator(): " + e);
					try {
						cursor.close();
					} catch (Throwable t) {
					}
					cursor = null;
					lastStatus = null;
				}
			}

			public DBIterator(byte prefixFilter[]) {
				this.prefix = prefixFilter;
				try {
					cursor = db.openCursor(null, null);
					keyEntry.setData(this.prefix);
					lastStatus = cursor.getSearchKeyRange(keyEntry, valEntry, null);
					// System.err.println("opening iterator: filter="+
					// Util.decodeString(prefixFilter)+
					// " status="+lastStatus+"; key="+keyEntry);

					if (lastStatus != null && lastStatus == OperationStatus.SUCCESS) {
						// check to see if the next value starts with the prefix
						if (Util.startsWithCI(keyEntry.getData(), this.prefix)) {
							loadValueFromEntries();
						} else {
							cursor.close();
							lastStatus = null;
						}
					} else {
						cursor.close();
					}
				} catch (Exception e) {
					System.err.println("Error in DBIterator(): " + e);
					try {
						cursor.close();
					} catch (Throwable t) {
					}
					cursor = null;
					lastStatus = null;
				}
			}

			public synchronized boolean hasMoreElements() {
				return lastStatus == OperationStatus.SUCCESS;
			}

			private void loadValueFromEntries() throws Exception {
				byte b[][] = { new byte[keyEntry.getSize()], new byte[valEntry.getSize()] };
				System.arraycopy(keyEntry.getData(), 0, b[0], 0, b[0].length);
				System.arraycopy(valEntry.getData(), 0, b[1], 0, b[1].length);
				this.currentValue = b;
			}

			public synchronized Object nextElement() throws java.util.NoSuchElementException {
				if (cursor == null || lastStatus == null || lastStatus != OperationStatus.SUCCESS)
					throw new java.util.NoSuchElementException();

				byte returnVal[][] = currentValue;

				// fetch the next item....
				try {
					lastStatus = cursor.getNext(keyEntry, valEntry, null);
					if (lastStatus == null || lastStatus != OperationStatus.SUCCESS) {
						lastStatus = null;
						cursor.close();
					} else {
						loadValueFromEntries();

						// check to see if the next value starts with the prefix
						if (prefix != null && !Util.startsWithCI(this.currentValue[0], this.prefix)) {
							cursor.close();
							lastStatus = null;
						}
					}
				} catch (Exception e) {
					System.err.println("Error scanning handles: " + e);
					e.printStackTrace(System.err);
					try {
						cursor.close();
					} catch (Throwable t) {
					}
					cursor = null;
					lastStatus = null;
				}
				return returnVal;
			}

			public void finalize() {
				cleanup();
			}

			void cleanup() {
				try {
					if (cursor != null)
						cursor.close();
				} catch (Throwable t) {
				}
			}

		}

	}

}
