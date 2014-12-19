package fr.ortolang.diffusion.tool.invoke;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

public class ToolJobInvocationResult {

	public enum Status {
		SUCCESS, ERROR
	}

	private String preview;
	private Map<String, String> files;
	private Status status;
	
	
	public ToolJobInvocationResult() throws JsonParseException, JsonMappingException, IOException {		
		this.preview = "";
		this.files = new HashMap<String,String>();
	}
	
	public void saveResult(Path path) throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonArrayBuilder filesJsonBuilder = Json.createArrayBuilder();
		for(Entry<String, String> entry : files.entrySet()) {
			filesJsonBuilder.add(Json.createObjectBuilder().add("name", entry.getKey()).add("url", entry.getValue()).build());
		}
		JsonArray resultJson = Json.createArrayBuilder()
				.add(Json.createObjectBuilder()
						.add("key", "preview")
						.add("type", "text-preview")
						.add("label", "Output Preview")
						.add("active", true)
						.add("content", this.preview)
						.build())
				.add(Json.createObjectBuilder()
						.add("key", "files")
						.add("type", "link-list")
						.add("label", "Generated file(s)")
						.add("active", false)
						.add("options", filesJsonBuilder.build())
						.build())
				.build();
		FileWriter file = new FileWriter(path.toString());                
		file.write(resultJson.toString());
		file.flush();
		file.close();
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Map<String, String> getFiles() {
		return files;
	}

	public void setFiles(Map<String, String> files) {
		this.files = files;
	}
	
	public void setFile(String filename, String filepath) {
		this.files.put(filename, filepath);
	}

	public String getPreview() {
		return preview;
	}

	public void setPreview(String preview) {
		this.preview = preview;
	}
	
}
