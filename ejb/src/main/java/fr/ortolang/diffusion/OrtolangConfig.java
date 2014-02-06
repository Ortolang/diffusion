package fr.ortolang.diffusion;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OrtolangConfig {

	private static Logger logger = Logger.getLogger(OrtolangConfig.class.getName());
	private static OrtolangConfig config;
	private Properties props;
	
	private OrtolangConfig(String configFilePath) throws Exception {
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

    private OrtolangConfig(URL configFileURL) throws Exception {
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

    public static synchronized OrtolangConfig getInstance() {
        try {
            if (config == null) {
                String configFilePath = System.getProperty("sourceplant.config");
                if (configFilePath != null && configFilePath.length() != 0) {
                    config = new OrtolangConfig(configFilePath);
                    logger.log(Level.INFO, "using custom config file : " + configFilePath);
                } else {
                    URL configFileURL = OrtolangConfig.class.getClassLoader().getResource("config.properties");
                    config = new OrtolangConfig(configFileURL);
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

//	private DiffusionConfig(Path configfile) throws Exception {
//		props = new Properties();
//		try (InputStream in = Files.newInputStream(configfile)) {
//			props.load(in);
//		} 
//	}
//
//	public static synchronized DiffusionConfig getInstance() {
//		try {
//			if (config == null) {
//				if (System.getProperty("diffusion.config") != null) {
//					Path configfile = Paths.get(System.getProperty("diffusion.config"));
//					if ( Files.exists(configfile) ) {
//						logger.info("using custom config file : " + configfile);
//						return new DiffusionConfig(configfile);
//					}
//				} 
//				Path configfile = Paths.get(DiffusionConfig.class.getClassLoader().getResource("config.properties").getPath());
//				logger.info("using default config file : " + configfile);
//				return new DiffusionConfig(configfile);
//			}
//			return config;
//		} catch (Exception e) {
//			logger.log(Level.SEVERE, "error while creating configuration", e);
//		}
//		return null;
//	}

	public String getProperty(String name) {
		return props.getProperty(name);
	}

}