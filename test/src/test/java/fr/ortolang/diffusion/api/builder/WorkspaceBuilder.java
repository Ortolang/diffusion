package fr.ortolang.diffusion.api.builder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class WorkspaceBuilder { //implements Runnable {
	
	private Logger logger = Logger.getLogger(WorkspaceBuilder.class.getName());

	private String wskey;
	private Path root;

	private Path data;
	private Path metadata;
	private Path objects;
	//private OrtolangRestClient client;
	
	public WorkspaceBuilder(String username, String password, String baseurl, String workspace, String root) {
		this.wskey = workspace;
		this.root = Paths.get(root);
		this.data = this.root.resolve("data");
		this.metadata = this.data.resolve("metadatas");
		this.objects = this.data.resolve("objects");
		
	    //client = new OrtolangRestClient(username, password, baseurl);
    }
	
	

//	@Override
//	public void run() {
//		try {
//			client.checkAuthentication();
//		} catch ( Exception e ) {
//			logger.log(Level.SEVERE, "authentication failed: " + e.getMessage(), e);
//		}
//		
//		try {
//			boolean exists = client.objectExists(wskey);
//			if ( exists ) {
//				logger.log(Level.WARNING, "Workspace with key [" + wskey + "] already exists, exit");
//				return;
//			}
//			client.createWorkspace(wskey, "bench", wskey + " complete name");
//		} catch ( Exception e ) { 
//			logger.log(Level.SEVERE, "unexpected error: " + e.getMessage(), e);
//		}
//
//		LocalFolderCollectionBuilder builder = new LocalFolderCollectionBuilder(client);
//		try {
//			Files.walkFileTree(objects, builder);
//		} catch (IOException e) {
//			logger.log(Level.WARNING, "Unexpected error during walking local collection: " + e.getMessage(), e);
//		}
//		
//		if(metadata.toFile().exists()) {
//			LocalMetadataFolderCollectionBuilder metadataBuilder = new LocalMetadataFolderCollectionBuilder(client);
//			try {
//				Files.walkFileTree(metadata, metadataBuilder);
//			} catch (IOException e) {
//				logger.log(Level.WARNING, "Unexpected error during walking local metadata folder: " + e.getMessage(), e);
//			}
//		}
//
//		client.close();
//	}
//
//	class LocalFolderCollectionBuilder implements FileVisitor<Path> {
//
//		private OrtolangRestClient client;
//		private int nbCollections = 0;
//		private int nbDataObjects = 0;
//		private long volume = 0;
//		
//		public LocalFolderCollectionBuilder(OrtolangRestClient client) {
//			this.client = client;
//		}
//
//		public long getTotalLength() {
//			return volume;
//		}
//
//		public int getNbCollections() {
//			return nbCollections;
//		}
//
//		public int getNbDataObjects() {
//			return nbDataObjects;
//		}
//
//		@Override
//		public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
//			try {
//				logger.log(Level.INFO, "Creating collection for path: " + objects.relativize(path));
//				if ( objects.relativize(path).toString().length() == 0 ) {
//					logger.log(Level.INFO, "ROOT Collection, nothing to do !");
//					return FileVisitResult.CONTINUE; 
//				}
//				client.writeCollection(wskey, objects.relativize(path).toString(), "A collection corresponding to provided folder " + path.getFileName());
//				nbCollections++;
//				return FileVisitResult.CONTINUE;
//			} catch ( Exception e ) {
//				logger.log(Level.WARNING, "unable to create collection, skipping sub directory of path: " + path);
//				return FileVisitResult.SKIP_SUBTREE;
//			}
//		}
//
//		@Override
//		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
//			try {
//				logger.log(Level.INFO, "Creating DataObject for file: " + objects.relativize(path));
//				File file = path.toFile();
//				client.writeDataObject(wskey, objects.relativize(path).toString(), "A data object for file " + file.getName(), file, null);
//				nbDataObjects++;
//				volume += file.length();
//				return FileVisitResult.CONTINUE;
//			} catch ( Exception e ) {
//				logger.log(Level.WARNING, "unable to create dataobject, continue after path: " + path);
//				return FileVisitResult.CONTINUE;
//			}
//		}
//
//		@Override
//		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//			logger.log(Level.WARNING, "Unable to create DataObject for file : " + file, exc);
//			return FileVisitResult.CONTINUE;
//		}
//
//		@Override
//		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//			return FileVisitResult.CONTINUE;
//		}
//
//	}
//
//	class LocalMetadataFolderCollectionBuilder implements FileVisitor<Path> {
//
//		private OrtolangRestClient client;
//		private int nbMetaDataObjects = 0;
//		private long volume = 0;
//		
//		public LocalMetadataFolderCollectionBuilder(OrtolangRestClient client) {
//			this.client = client;
//		}
//
//		public long getTotalLength() {
//			return volume;
//		}
//
//		public int getNbDataObjects() {
//			return nbMetaDataObjects;
//		}
//
//		@Override
//		public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
//			logger.log(Level.FINE, "Going to the folder for path: " + metadata.relativize(path));
//			return FileVisitResult.CONTINUE;
//		}
//
//		@Override
//		public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
//			try {
//				logger.log(Level.INFO, "Creating MetaData Object for file: " + metadata.relativize(path));
//				
//				File file = path.toFile();
//				Path parentPath = path.getParent();
//				
//				String mdname = file.getName();
//				String mdformat = "unknown";
//				if (mdname.indexOf("[") == 0 && mdname.indexOf("]") >= 0) {
//					mdformat = mdname.substring(1, mdname.indexOf("]")).trim();
//					mdname = mdname.substring(mdname.indexOf("]")+1).trim();
//				}
//				
//				logger.log(Level.INFO, "Parent path : "+parentPath);
//				logger.log(Level.INFO, "Creating Metadata object : name:"+mdname+" : format:"+mdformat);
//				
//				logger.log(Level.INFO, "Path : "+metadata.relativize(parentPath).toString());
//				client.writeMetaData(wskey, metadata.relativize(parentPath).toString(), mdname,  mdformat, file);
//				nbMetaDataObjects++;
//				volume += file.length();
//				return FileVisitResult.CONTINUE;
//			} catch ( Exception e ) {
//				logger.log(Level.WARNING, "unable to create metadata object, continue after path: " + path);
//				logger.log(Level.WARNING, "", e.fillInStackTrace());
//				return FileVisitResult.CONTINUE;
//			}
//		}
//
//		@Override
//		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
//			logger.log(Level.WARNING, "Unable to create MetaDataObject for file : " + file, exc);
//			return FileVisitResult.CONTINUE;
//		}
//
//		@Override
//		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
//			return FileVisitResult.CONTINUE;
//		}
//
//	}
//	
//	public static void main(String[] args) {
//		WorkspaceBuilder sample1 = new WorkspaceBuilder("user1", "tagada", "http://localhost:8080/api/rest", "frantext13", "src/test/resources/samples/bag");
//		Thread t = new Thread(sample1);
//		t.start();
//	}

}
