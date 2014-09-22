package fr.ortolang.diffusion.api.oaipmh.model;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.model.oaipmh.Metadata;
import com.lyncode.xoai.model.xoai.XOAIMetadata;
import com.lyncode.xoai.xml.XmlWriter;

import fr.ortolang.diffusion.api.oaipmh.xml.DiffusionEchoElement;

/**
 * Same as Metadata from XOAI but with DiffusionEchoElement.
 * 
 * @author cyril
 *
 */
public class DiffusionMetadata extends Metadata {
	protected String input;
	
	public DiffusionMetadata(InputStream value) throws IOException {
		super((XOAIMetadata) null);
		
		this.input = IOUtils.toString(value);
	}

	@Override
    public void write(XmlWriter writer) throws XmlWriteException {
        if (this.value != null)
            this.value.write(writer);
        else {
            DiffusionEchoElement elem = new DiffusionEchoElement(this.input);
            elem.write(writer);
        }
    }
}
