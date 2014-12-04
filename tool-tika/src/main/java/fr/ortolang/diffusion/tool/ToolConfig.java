package fr.ortolang.diffusion.tool;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.ortolang.diffusion.tool.ToolConfig;

public class ToolConfig {
	private static Logger logger = Logger.getLogger(ToolConfig.class.getName());
	private static ToolConfig config;
	private Properties props;
	
	private ToolConfig(String configFilePath) throws Exception {
        props = new Properties();
        FileInputStream in = null;
        try {
        	in = new FileInputStream(configFilePath);
        	props.load(in);
        } finally {
        	if ( in != null ) {
        		in.close();
        	}
        }
    }

    private ToolConfig(URL configFileURL) throws Exception {
        props = new Properties();
        InputStream in = null;
        try {
        	in = configFileURL.openStream();
        	props.load(in);
        } finally {
        	if ( in != null ) {
        		in.close();
        	}
        }
    }

    public static synchronized ToolConfig getInstance() {
        try {
            if (config == null) {
                String configFilePath = System.getProperty("tool.config.file");
                if (configFilePath != null && configFilePath.length() != 0) {
                    config = new ToolConfig(configFilePath);
                    logger.log(Level.INFO, "using custom config file : " + configFilePath);
                } else {
                    URL configFileURL = ToolConfig.class.getClassLoader().getResource("config.properties");
                    config = new ToolConfig(configFileURL);
                    logger.log(Level.INFO, "using default config file : " + configFileURL.getPath());
                }
            }

            return config;
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "unable to load configuration", e);
        }
        return null;
    }

	public String getProperty(String name) {
		return props.getProperty(name);
	}
	
	public Properties getProperties() {
		return props;
	}

}
