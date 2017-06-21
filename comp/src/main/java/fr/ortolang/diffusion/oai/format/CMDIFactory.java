package fr.ortolang.diffusion.oai.format;

import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class CMDIFactory {

    private static final Logger LOGGER = Logger.getLogger(CMDIFactory.class.getName());
    
	public static CMDI buildFromItem(String item) {
		CMDI cmdi = new CMDI();
		
		StringReader reader = new StringReader(item);
        JsonReader jsonReader = Json.createReader(reader);
        JsonObject jsonDoc = jsonReader.readObject();
        
		try {
			//TODO implement from JSON
		} catch(Exception e) {
        	LOGGER.log(Level.SEVERE, "unable to build CMDI from item", e);
        } finally {
            jsonReader.close();
            reader.close();
        }
		
        return cmdi;
	}
	

	public static CMDI convertFromJsonOlac(String json) {
		CMDI cmdi = new CMDI();
		StringReader reader = new StringReader(json);
		JsonReader jsonReader = Json.createReader(reader);

		try {
			JsonObject jsonDoc = jsonReader.readObject();
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "unable to build CMDI from OLAC json", e);
		} finally {
			jsonReader.close();
			reader.close();
		}

		return cmdi;
	}
}
