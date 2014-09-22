package fr.ortolang.diffusion.runtime.task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import fr.ortolang.diffusion.OrtolangServiceLocator;
import fr.ortolang.diffusion.store.binary.BinaryStoreService;

public class ExtractTask extends Task {

	public static final String HASH_PARAM_NAME = "hash";
	public static final String EXTRACT_PATH_PARAM_NAME = "path_param";
	
	private Logger logger = Logger.getLogger(ExtractTask.class.getName());
	private BinaryStoreService store;

	public ExtractTask() {
		super();
	}

	public BinaryStoreService getBinaryStore() throws Exception {
		if (store == null) {
			store = (BinaryStoreService) OrtolangServiceLocator.findService(BinaryStoreService.SERVICE_NAME);
		}
		return store;
	}

	protected void setBinaryStore(BinaryStoreService store) {
		this.store = store;
	}

	@Override
	public TaskState execute() {
		this.log("Starting ExtractArchive task");
		try {
			if (!getParams().containsKey(HASH_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + HASH_PARAM_NAME + " parameter");
				this.log("ExtractArchive task error : mandatory parameter " + HASH_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			if (!getParams().containsKey(EXTRACT_PATH_PARAM_NAME)) {
				logger.log(Level.WARNING, "unable to find mandatory " + EXTRACT_PATH_PARAM_NAME + " parameter");
				this.log("ExtractArchive task error : mandatory parameter " + EXTRACT_PATH_PARAM_NAME + " not found in context!!");
				return TaskState.ERROR;
			}
			logger.log(Level.INFO, HASH_PARAM_NAME + " parameter found: " + getParam(HASH_PARAM_NAME));
			String hash = getParam(HASH_PARAM_NAME);
			Path file = Files.createTempFile("extract-task.", ".zip");
			Path base = Files.createTempDirectory("extract-task.");
			String type = store.type(hash);
			if (type.equals("application/zip")) {
				this.log("application/zip mime type found, expending content");
				Files.copy(store.get(hash), file, StandardCopyOption.REPLACE_EXISTING);
				try (ZipFile zip = new ZipFile(file.toFile())) {
					Enumeration<? extends ZipEntry> entries = zip.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String entryName = entry.getName();
						entryName = entryName.replace('\\', '/');
						logger.log(Level.FINE, "entry found: " + entryName);

						Path dest = base.resolve(entryName);
						if (entryName.endsWith("/")) {
							if (!dest.toFile().isDirectory() && !dest.toFile().mkdirs()) {
								logger.log(Level.WARNING, "error creating directory for entry: " + entryName);
								this.log("error creating directory for entry: " + entryName);
								return TaskState.ERROR;
							}
							continue;
						} else if (entryName.indexOf('/') != -1) {
							if (!dest.getParent().toFile().isDirectory() && !dest.getParent().toFile().mkdirs()) {
								logger.log(Level.WARNING, "error creating parent directory for entry: " + entryName);
								this.log("error creating parent directory for entry: " + entryName);
								return TaskState.ERROR;
							}
						}
						Files.copy(zip.getInputStream(entry), dest);
						this.log("entry extracted: " + entryName);
						logger.log(Level.FINE, "entry extracted: " + entryName);
					}
				}
				this.log("zip file content expended into folder: " + base.toString());
			} else {
				this.log("unexpected mime type [" + store.type(hash) + "] for hash [" + hash + "], expected [application/zip] !");
				return TaskState.ERROR;
			}
			Files.delete(file);
			this.getParams().put(getParam(EXTRACT_PATH_PARAM_NAME), base.toString());
			this.log("ExtractArchive task ended");
			return TaskState.COMPLETED;
		} catch (Exception e) {
			logger.log(Level.SEVERE, "error during task execution" + e);
			this.log("unexpected error occured during task execution: " + e.getMessage() + ", see server log for further details");
			return TaskState.ERROR;
		}
	}

}
