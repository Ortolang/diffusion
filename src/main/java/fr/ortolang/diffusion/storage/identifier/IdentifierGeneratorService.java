package fr.ortolang.diffusion.storage.identifier;

import java.io.InputStream;

/**
 * <p>
 * <b>Identifier Generator Service</b> for ORTOLANG Diffusion Server.<br/>
 * This identifier generator service is an internal service dedicated to content based object identifier generation.
 * It has to generate an identifier based on a content hash with a minimal collision probability and the fastest 
 * generation time. SHA1 seems to be a good choice. 
 * </p>
 * 
 * @author Jerome Blanchard <jayblanc@gmail.com>
 * @version 1.0
 */
public interface IdentifierGeneratorService {
	
	/**
	 * Generate an identifier for the given stream content.
	 * 
	 * @param stream the content input stream
	 * @return A String representation of the identifier
	 * @throws IdentifierGeneratorException
	 */
	String generate(InputStream stream) throws IdentifierGeneratorException;

}
