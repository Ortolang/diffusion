package fr.ortolang.diffusion.rest.api.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import fr.ortolang.diffusion.rest.api.DecoratorManager;
import fr.ortolang.diffusion.rest.api.OrtolangObjectsRepresentation;

@Provider
@Produces(MediaType.TEXT_HTML)
public class OrtolangObjectsRepresentationBodyWriter implements MessageBodyWriter<OrtolangObjectsRepresentation> {
	
	private Logger logger = Logger.getLogger(OrtolangObjectsRepresentationBodyWriter.class.getName());

	@Override
	public long getSize(OrtolangObjectsRepresentation opbject, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediatype) {
		return -1;
	}

	@Override
	public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
		return clazz == OrtolangObjectsRepresentation.class;
	}

	@Override
	public void writeTo(OrtolangObjectsRepresentation object, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> map,
			OutputStream out) throws IOException, WebApplicationException {
		StringBuffer buffer = new StringBuffer();
		buffer.append("<div class=\"oobjects\">");
		buffer.append("<div class=\"content\">");
		buffer.append("<ul>");
		for ( Entry<String, List<Link>> entry : object.getEntries().entrySet() ) {
			buffer.append("<li><span class=\"entry\">").append(entry.getKey()).append(" </span>");
			for ( Link link : entry.getValue() ) {
				buffer.append("[<a class=\"link\" href=\"").append(link.getUri()).append("\">").append(link.getRel()).append("</a>] - ");	
			}
			buffer.append("</li>");
		}
		buffer.append("</ul>");
		buffer.append("</div");
		buffer.append("<div class=\"informations\">");
		buffer.append("showing items from ").append(object.getStart());
		buffer.append(" to ").append(object.getStart() + object.getSize());
		buffer.append(" on a total of ").append(object.getTotalSize());
		buffer.append("</div>");
		buffer.append("<div class=\"navigation\">");
		for ( Link link : object.getLinks() ) {
			buffer.append("[<a class=\"link\" href=\"").append(link.getUri()).append("\">").append(link.getRel()).append("</a>] - ");	
		}
		buffer.append("</div");
		buffer.append("</div>");
		String output = buffer.toString();
		output = DecoratorManager.getInstance().decorate("decorators/base.html", output);
		out.write(output.getBytes());
	}
}