package fr.ortolang.diffusion.api.usecase;


public class ImportBagItAndPublishUseCase {

	private static final String DEFAULT_BAGS_FOLDER = "src/test/resources/samples/bagit";
	
//	private static Logger logger = Logger.getLogger(ImportBagItAndPublishUseCase.class.getName());
//	
//	@Test
//	public void bench() throws IOException {
//		
//		File bags = getBagsFolder();
//
//		logger.log(Level.INFO, "Starting importing bags folder: " + bags.getAbsolutePath());
//		OrtolangRestClient client = new OrtolangRestClient("root", "tagada54", "http://localhost:8080/api/rest");
//		
//		List<File> files = Arrays.asList(bags.listFiles());
//		
//		for ( File bag : files) {
//			if ( !bag.isDirectory() ) {
//				logger.log(Level.INFO, "Creating new import-workspace process for bag: " + bag.getName());
//				String key = bag.getName().replaceFirst(".zip", "");
//				Map<String, String> params = new HashMap<String, String> ();
//				params.put("workspace-key", key);
//				params.put("workspace-name", "Workspace of bag " + bag.getName());
//				params.put("workspace-type", "benchmark");
//				Map<String, File> attachments = new HashMap<String, File> ();
//				attachments.put("bag-hash", bag);
//				try {
//					String pkey = client.createProcess("import-workspace", "Workspace import for bag " + bag.getName(), params, attachments);
//					logger.log(Level.INFO, "process created with key : " + pkey + " watching process progression");
//					boolean finished = false;
//					while ( !finished ) {
//						try {
//							logger.log(Level.INFO, "Waiting for the import process...");
//							Thread.sleep(30000);
//						} catch ( InterruptedException e ) {
//							logger.log(Level.WARNING, "thread sleep interrupted: " + e.getMessage());
//						}
//						JsonObject process = client.getProcess(pkey);
//						String state = process.getString("state");
//						if ( state.equals("ABORTED") || state.equals("COMPLETED") ) {
//							logger.log(Level.INFO, "process ended, process log: \r\n" + process.getString("log"));
//							finished = true;
//						} else {
//							logger.log(Level.INFO, "process in progress, waiting...");
//						}
//					}
//				} catch ( Exception e ) {
//					e.printStackTrace();
//					logger.log(Level.WARNING, "unable to create process for bag " + bag.getName());
//					continue;
//				}
//				
//				String head = null;
//				try {
//					JsonObject wsJsonObject = client.readWorkspace(key);
//					head = wsJsonObject.getJsonString("head").getString();
//				} catch ( Exception e ) {
//					e.printStackTrace();
//					logger.log(Level.WARNING, "unable to read workspace for bag " + bag.getName());
//					continue;
//				}
//				
//				StringBuffer keys = listKeys(client, head, new StringBuffer());
//				keys.deleteCharAt(keys.length()-1);
//				logger.log(Level.INFO, "keys " + keys);
//				
//				// Process for published
//				Map<String, String> paramsPub = new HashMap<String, String> ();
//				paramsPub.put("keys", keys.toString());
//				Map<String, File> attachmentsPub = new HashMap<String, File> ();
//				try {
//					String pkey = client.createProcess("simple-publication", "Release " + bag.getName(), paramsPub, attachmentsPub);
//					logger.log(Level.INFO, "process created with key : " + pkey + " watching process progression");
//					boolean finished = false;
//					while ( !finished ) {
//						try {
//							logger.log(Level.INFO, "Waiting for the publication process...");
//							Thread.sleep(30000);
//						} catch ( InterruptedException e ) {
//							logger.log(Level.WARNING, "thread sleep interrupted: " + e.getMessage());
//						}
//						JsonObject process = client.getProcess(pkey);
//						String state = process.getString("state");
//						if ( state.equals("ABORTED") || state.equals("COMPLETED") ) {
//							logger.log(Level.INFO, "process ended, process log: \r\n" + process.getString("log"));
//							finished = true;
//						} else {
//							logger.log(Level.INFO, "process in progress, waiting...");
//						}
//					}
//				} catch ( Exception e ) {
//					e.printStackTrace();
//					logger.log(Level.WARNING, "unable to publish workspace for bag " + bag.getName());
//					continue;
//				}
//				
//			}
//		}
//	}
//	
//
//	private File getBagsFolder() {
//		File folder = Paths.get(DEFAULT_BAGS_FOLDER).toFile();
//		String property = System.getProperty("bags.folder");
//		if (property != null && property.length() != 0) {
//			folder = Paths.get(property).toFile();
//		} 
//		return folder;
//	}
//	
//
//	protected StringBuffer listKeys(OrtolangRestClient client, String key, StringBuffer buffer) {
//		
//		try {
//			JsonObject objectJsonObject = client.getObject(key);
//			String type = objectJsonObject.getJsonString("type").getString();
//			
//			buffer.append(key).append(",");
//
//			JsonObject oobject = objectJsonObject.getJsonObject("object");
//			
//			if(type.equals("collection")) {
//				JsonArray elements = oobject.getJsonArray("elements");
//				
//				for(JsonValue element : elements) {
//					if(element.getValueType() == ValueType.OBJECT) {
//						listKeys(client, ((JsonObject) element).getString("key"), buffer);
//					}
//				}
//			}
//			
//			JsonArray metadatas = oobject.getJsonArray("metadatas");
//			for(JsonValue metadata : metadatas) {
//				if(metadata.getValueType() == ValueType.OBJECT) {
//					buffer.append(((JsonObject) metadata).getString("key")).append(",");
//				}
//			}
//			
//		} catch (OrtolangRestClientException e) {
//			e.printStackTrace();
//			logger.log(Level.WARNING, "unable to get object for key "+key);
//		}
//		return buffer;
//	}

	
}
