package fr.ortolang.diffusion.rest.api.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.rest.api.OrtolangObjectRepresentation;

@Provider
@Produces(MediaType.TEXT_HTML)
public class OrtolangObjectRepresentationBodyWriter implements MessageBodyWriter<OrtolangObjectRepresentation> {

	@Override
	public long getSize(OrtolangObjectRepresentation opbject, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediatype) {
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
		return clazz == OrtolangObjectRepresentation.class;
	}

	@Override
	public void writeTo(OrtolangObjectRepresentation object, Class<?> clazz, Type type, Annotation[] annotation, MediaType mediaType, MultivaluedMap<String, Object> map,
			OutputStream out) throws IOException, WebApplicationException {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<div class=\"oobject\" id=\"").append(object.getKey()).append("\">");
		buffer.append("<div class=\"content\">");
		buffer.append("<div class=\"key\">key: ").append(object.getKey()).append("</div>");
		buffer.append("<div class=\"service\">service: ").append(object.getService()).append("</div>");
		buffer.append("<div class=\"type\">type: ").append(object.getType()).append("</div>");
		buffer.append("</div>");
		buffer.append("<div class=\"navigation\">");
		for ( Link link : object.getLinks() ) {
			buffer.append("[<a class=\"link\" href=\"").append(link.getUri()).append("\">").append(link.getRel()).append("</a>] - ");
		}
		buffer.append("</div>");
		buffer.append("</div>");
		out.write(buffer.toString().getBytes());
	}
}