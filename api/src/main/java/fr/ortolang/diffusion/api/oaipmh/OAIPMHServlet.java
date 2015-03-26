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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import fr.ortolang.diffusion.api.oaipmh.dataprovider.DiffusionDataProvider;
import fr.ortolang.diffusion.api.oaipmh.repository.DiffusionItemRepository;
import fr.ortolang.diffusion.api.rest.DiffusionUriBuilder;
import fr.ortolang.diffusion.core.CoreService;
import fr.ortolang.diffusion.search.SearchService;

@SuppressWarnings("serial")
public class OAIPMHServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(OAIPMHServlet.class.getName());

	@EJB
	private static SearchService search;
	@EJB
	private static CoreService core;
	
    private static Context context;
    private static Repository repository;
    private static DiffusionDataProvider dataProvider;
    
	@Override
	public void init(ServletConfig config) throws ServletException {
		LOGGER.log(Level.FINE, "OAI PMH Initialized");
		
		String metadataFormatsStr = OrtolangConfig.getInstance().getProperty("oai.metadata.format");
		String[] metadataFormats = metadataFormatsStr.split(",");
		for(String metadataFormat : metadataFormats) {
			context = new Context()
			.withMetadataFormat(MetadataFormat.metadataFormat(OrtolangConfig.getInstance().getProperty("oai.metadata.format."+metadataFormat+".prefix"))
					.withNamespace(OrtolangConfig.getInstance().getProperty("oai.metadata.format."+metadataFormat+".namespace"))
					.withSchemaLocation(OrtolangConfig.getInstance().getProperty("oai.metadata.format."+metadataFormat+".schemaLocation"))
					);
		}
		
		InMemorySetRepository setRepository = new InMemorySetRepository();
		DiffusionItemRepository itemRepository = new DiffusionItemRepository(search, core);
		
		UTCDateProvider dateProvider = new UTCDateProvider();
		String earliestDateStr = OrtolangConfig.getInstance().getProperty("oai.earliestdate");
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
			.withAdminEmail(OrtolangConfig.getInstance().getProperty("oai.adminemail")) //TODO ajouter une valeur par defaut dans la methode getProperty
			.withBaseUrl(DiffusionUriBuilder.getBaseUriBuilder().path(OrtolangConfig.getInstance().getProperty("oai.base.path")).build().toString())
			.withRepositoryName(OrtolangConfig.getInstance().getProperty("oai.repositoryname"))
			.withDeleteMethod(DeletedRecord.NO)
			.withEarliestDate(earliestDate)
//			.withDescription("ORTOLANG repository") //TODO add a description (oai-identifier?olac-description?)
			.withGranularity(Granularity.Day)
			.withMaxListIdentifiers(Integer.parseInt(OrtolangConfig.getInstance().getProperty("oai.maxlistidentifiers")))
			.withMaxListRecords(Integer.parseInt(OrtolangConfig.getInstance().getProperty("oai.maxlistrecords")))
			.withMaxListSets(Integer.parseInt(OrtolangConfig.getInstance().getProperty("oai.maxlistsets")));
		
		repository = new Repository()
		    .withSetRepository(setRepository)
		    .withItemRepository(itemRepository)
		    .withResumptionTokenFormatter(new SimpleResumptionTokenFormat())
		    .withConfiguration(repositoryConfiguration);

		dataProvider = new DiffusionDataProvider(context, repository);
	}


	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		// 
		Map<String,List<String>> reqParam = toParameters(req.getParameterMap());
		
		OAIRequest request = new OAIRequest(reqParam);
		
		// DataProvider
		OAIPMH response = null;
		try {
			response = dataProvider.handle(request);
		} catch (OAIException e1) {
			LOGGER.log(Level.WARNING, e1.getMessage(), e1.fillInStackTrace());
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e1.getMessage());
			return;
		}
		
		if(response!=null) {
			String result = "";
			
			try {
				result = write(response);
			} catch (XMLStreamException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return;
			} catch (XmlWriteException e) {
				LOGGER.log(Level.WARNING, e.getMessage(), e.fillInStackTrace());
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				return;
			}
			
			resp.setContentType("application/xml; charset=utf-8");
			resp.setCharacterEncoding("UTF-8");

			PrintWriter writer = resp.getWriter();

			writer.print(result);

			writer.flush();
			writer.close();
			
			return;
		}
		
		LOGGER.log(Level.WARNING, "OAIPMH XMl object is null");
		resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No valid response to send");
		return;
	}

    protected String write(final XmlWritable handle) throws XMLStreamException, XmlWriteException {
        return OAIPMHServlet.toString(new XmlWritable() {
            @Override
            public void write(XmlWriter writer) throws XmlWriteException {
                    writer.write(handle);
            }
        });
    }
    
    public static String toString (XmlWritable writable) throws XMLStreamException, XmlWriteException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter(outputStream, new WriterContext(Granularity.Day, new SimpleResumptionTokenFormat()));
        writable.write(writer);
        writer.close();
        return outputStream.toString();
    }
    
    protected Map<String,List<String>> toParameters(Map<String, String[]> mParam) {
		Map<String,List<String>> reqParam = new HashMap<String,List<String>>();
    	for(Map.Entry<String, String[]> entry : mParam.entrySet()) {
			List<String> lValues = new ArrayList<String>();
			for(String value : entry.getValue()) {
				lValues.add(value);
			}
			reqParam.put(entry.getKey(), lValues);
		}
    	return reqParam;
    }
    

}
