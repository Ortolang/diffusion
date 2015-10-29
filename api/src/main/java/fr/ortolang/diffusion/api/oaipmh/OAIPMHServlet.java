package fr.ortolang.diffusion.api.oaipmh;

/*
 * #%L
 * ORTOLANG
 * A online network structure for hosting language resources and tools.
 * 
 * Jean-Marie Pierrel / ATILF UMR 7118 - CNRS / Université de Lorraine
 * Etienne Petitjean / ATILF UMR 7118 - CNRS
 * Jérôme Blanchard / ATILF UMR 7118 - CNRS
 * Bertrand Gaiffe / ATILF UMR 7118 - CNRS
 * Cyril Pestel / ATILF UMR 7118 - CNRS
 * Marie Tonnelier / ATILF UMR 7118 - CNRS
 * Ulrike Fleury / ATILF UMR 7118 - CNRS
 * Frédéric Pierre / ATILF UMR 7118 - CNRS
 * Céline Moro / ATILF UMR 7118 - CNRS
 *  
 * This work is based on work done in the equipex ORTOLANG (http://www.ortolang.fr/), by several Ortolang contributors (mainly CNRTL and SLDR)
 * ORTOLANG is funded by the French State program "Investissements d'Avenir" ANR-11-EQPX-0032
 * %%
 * Copyright (C) 2013 - 2015 Ortolang Team
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;

import com.lyncode.xml.exceptions.XmlWriteException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.model.MetadataFormat;
import com.lyncode.xoai.dataprovider.parameters.OAIRequest;
import com.lyncode.xoai.dataprovider.repository.InMemorySetRepository;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.dataprovider.repository.RepositoryConfiguration;
import com.lyncode.xoai.model.oaipmh.DeletedRecord;
import com.lyncode.xoai.model.oaipmh.Granularity;
import com.lyncode.xoai.model.oaipmh.OAIPMH;
import com.lyncode.xoai.services.impl.SimpleResumptionTokenFormat;
import com.lyncode.xoai.services.impl.UTCDateProvider;
import com.lyncode.xoai.xml.XmlWritable;
import com.lyncode.xoai.xml.XmlWriter;
import com.lyncode.xoai.xml.XmlWriter.WriterContext;

import fr.ortolang.diffusion.OrtolangConfig;
import fr.ortolang.diffusion.api.ApiUriBuilder;
import fr.ortolang.diffusion.api.oaipmh.dataprovider.DiffusionDataProvider;
import fr.ortolang.diffusion.api.oaipmh.repository.DiffusionItemRepository;
import fr.ortolang.diffusion.search.SearchService;

@Path("/oai")
@Produces({ MediaType.APPLICATION_XML })
public class OAIPMHServlet {

	@EJB
	private static SearchService search;

	private static Context context;
	private static Repository repository;
	private static DiffusionDataProvider dataProvider;

	private static final Logger LOGGER = Logger.getLogger(OAIPMHServlet.class.getName());

	@GET
	@Path("/")
	public Response oai(@QueryParam("verb") String verb
	        , @QueryParam("identifier") String identifier
	        , @QueryParam("metadataPrefix") String metadataPrefix
	        , @QueryParam("from") String from
	        , @QueryParam("until") String until) {
		
		context = new Context();
		context.withMetadataFormat(MetadataFormat.metadataFormat("oai_dc").withNamespace("http://www.openarchives.org/OAI/2.0/oai_dc/").withSchemaLocation("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"));
		context.withMetadataFormat(MetadataFormat.metadataFormat("olac").withNamespace("http://www.language-archives.org/OLAC/1.1/").withSchemaLocation("http://www.language-archives.org/OLAC/1.1/olac.xsd"));
		
		InMemorySetRepository setRepository = new InMemorySetRepository();
		setRepository.doesNotSupportSets();
		DiffusionItemRepository itemRepository = new DiffusionItemRepository(search);
		
		UTCDateProvider dateProvider = new UTCDateProvider();
		String earliestDateStr = "2014-08-12";
		Date earliestDate = null;
		try {
			earliestDate = dateProvider.parse(earliestDateStr, Granularity.Day);
		} catch (ParseException e) {
			LOGGER.log(Level.SEVERE, "unable to parse date", e);
		}
		if(earliestDate==null) {
			try {
				earliestDate = dateProvider.parse("2014-08-10", Granularity.Day);
			} catch (ParseException e) {
				earliestDate = new Date();
				LOGGER.log(Level.SEVERE, "unable to parse date", e);
			}
		}
		
		RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration()
			.withAdminEmail("contact@ortolang.fr")
			.withBaseUrl(ApiUriBuilder.getApiUriBuilder().path(OrtolangConfig.getInstance().getProperty(OrtolangConfig.Property.API_PATH_OAI)).build().toString())
			.withRepositoryName("ORTOLANG Repository")
			.withDescription(description())
			.withDeleteMethod(DeletedRecord.NO)
			.withEarliestDate(earliestDate)
			.withGranularity(Granularity.Day)
			.withMaxListIdentifiers(100)
			.withMaxListRecords(100)
			.withMaxListSets(100);
		
		repository = new Repository()
		    .withSetRepository(setRepository)
		    .withItemRepository(itemRepository)
		    .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
		    .withConfiguration(repositoryConfiguration);

		dataProvider = new DiffusionDataProvider(context, repository);
		
		//
		Map<String, List<String>> reqParam = new HashMap<String, List<String>>();
		OAIPMHServlet.putParameter("verb", verb, reqParam);
		OAIPMHServlet.putParameter("identifier", identifier, reqParam);
		OAIPMHServlet.putParameter("metadataPrefix", metadataPrefix, reqParam);
        OAIPMHServlet.putParameter("from", from, reqParam);
        OAIPMHServlet.putParameter("until", until, reqParam);
		
		OAIRequest oaiRequest = new OAIRequest(reqParam);

		OAIPMH response = null;
		try {
			response = dataProvider.handle(oaiRequest);
		} catch (OAIException e1) {
			LOGGER.log(Level.WARNING, e1.getMessage(), e1.fillInStackTrace());
//			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
//			return;
			return Response.serverError().build();
		}

		if (response != null) {

			try {
				return Response.ok(write(response)).header("Content-Type", "text/xml")
						.header("Character-Encoding", "UTF-8").build();
			} catch (XMLStreamException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
//				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
//				return;
				return Response.serverError().build();
			} catch (XmlWriteException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
//				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
//				return;
				return Response.serverError().build();
			}
		}

		LOGGER.log(Level.WARNING, "OAIPMH XMl object is null");
//		resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No valid response to send");
//		return;
		return Response.serverError().build();
	}
	
	protected String write(final XmlWritable handle) throws XMLStreamException, XmlWriteException {
		return OAIPMHServlet.toString(new XmlWritable() {
			@Override
			public void write(XmlWriter writer) throws XmlWriteException {
				writer.write(handle);
			}
		});
	}

	public static String toString(XmlWritable writable) throws XMLStreamException, XmlWriteException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		XmlWriter writer = new XmlWriter(outputStream, new WriterContext(Granularity.Day, new SimpleResumptionTokenFormat()));
		writable.write(writer);
		writer.close();
		return outputStream.toString();
	}

	protected static void putParameter(String name, String value, Map<String, List<String>> reqParam) {
      List<String> verbValues = new ArrayList<String>();
      verbValues.add(value);
      reqParam.put(name, verbValues);
	}
	
	protected static String description() {
	    return new StringBuilder().append("<oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"")
	        .append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
	        .append(" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">")
	        .append("<scheme>oai</scheme>")
	        .append("<repositoryIdentifier>www.ortolang.fr</repositoryIdentifier>")
	        .append("<delimiter>:</delimiter>")
	        .append("<sampleIdentifier>oai:ortolang.fr:dede</sampleIdentifier>")
	        .append("</oai-identifier>").toString();
	    
	}
}
