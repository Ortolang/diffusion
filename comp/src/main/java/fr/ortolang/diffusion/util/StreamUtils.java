package fr.ortolang.diffusion.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

public class StreamUtils {

	private static final Logger LOGGER = Logger.getLogger(StreamUtils.class.getName());

	public static final String getContent(InputStream is) throws IOException {
        String content = null;
        try {
            content = IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "  unable to get content from stream", e);
        } finally {
            is.close();
        }
        return content;
    }

    public static final String getContent(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }
}
