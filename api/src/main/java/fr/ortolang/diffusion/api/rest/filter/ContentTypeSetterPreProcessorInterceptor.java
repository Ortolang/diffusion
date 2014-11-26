package fr.ortolang.diffusion.api.rest.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;

@Provider
@ServerInterceptor
public class ContentTypeSetterPreProcessorInterceptor implements PreProcessInterceptor {
	
	private Logger logger = Logger.getLogger(ContentTypeSetterPreProcessorInterceptor.class.getName());

	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethodInvoker method) throws Failure, WebApplicationException {
		logger.log(Level.INFO, "content type setter for Input Part");
		request.setAttribute(InputPart.DEFAULT_CHARSET_PROPERTY, "UTF-8");
		return null;
	}

}