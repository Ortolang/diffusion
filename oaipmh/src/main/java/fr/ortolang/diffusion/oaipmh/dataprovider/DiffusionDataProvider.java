package fr.ortolang.diffusion.oaipmh.dataprovider;

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

import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.From;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.Identifier;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.MetadataPrefix;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.ResumptionToken;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.Set;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.Until;
import static com.lyncode.xoai.dataprovider.parameters.OAIRequest.Parameter.Verb;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.lyncode.builder.Builder;
import com.lyncode.xoai.dataprovider.DataProvider;
import com.lyncode.xoai.dataprovider.exceptions.BadArgumentException;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.DuplicateDefinitionException;
import com.lyncode.xoai.dataprovider.exceptions.HandlerException;
import com.lyncode.xoai.dataprovider.exceptions.IllegalVerbException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.exceptions.UnknownParameterException;
import com.lyncode.xoai.dataprovider.handlers.ErrorHandler;
import com.lyncode.xoai.dataprovider.handlers.IdentifyHandler;
import com.lyncode.xoai.dataprovider.handlers.ListSetsHandler;
import com.lyncode.xoai.dataprovider.model.Context;
import com.lyncode.xoai.dataprovider.parameters.OAICompiledRequest;
import com.lyncode.xoai.dataprovider.parameters.OAIRequest;
import com.lyncode.xoai.dataprovider.repository.Repository;
import com.lyncode.xoai.exceptions.InvalidResumptionTokenException;
import com.lyncode.xoai.model.oaipmh.OAIPMH;
import com.lyncode.xoai.model.oaipmh.Request;
import com.lyncode.xoai.services.api.DateProvider;
import com.lyncode.xoai.services.impl.UTCDateProvider;

import fr.ortolang.diffusion.oaipmh.handlers.DiffusionGetRecordHandler;
import fr.ortolang.diffusion.oaipmh.handlers.DiffusionListIdentifiersHandler;
import fr.ortolang.diffusion.oaipmh.handlers.DiffusionListMetadataFormatsHandler;
import fr.ortolang.diffusion.oaipmh.handlers.DiffusionListRecordsHandler;

public class DiffusionDataProvider {
	private Logger log = Logger.getLogger(this.getClass().getName());

    public static DataProvider dataProvider (Context context, Repository repository) {
        return new DataProvider(context, repository);
    }

    private Repository repository;
    private DateProvider dateProvider;

    private final IdentifyHandler identifyHandler;
    private final DiffusionGetRecordHandler getRecordHandler;
    private final ListSetsHandler listSetsHandler;
    private final DiffusionListRecordsHandler listRecordsHandler;
    private final DiffusionListIdentifiersHandler listIdentifiersHandler;
    private final DiffusionListMetadataFormatsHandler listMetadataFormatsHandler;
    private final ErrorHandler errorsHandler;

    public DiffusionDataProvider (Context context, Repository repository) {
        this.repository = repository;
        this.dateProvider = new UTCDateProvider();

        this.identifyHandler = new IdentifyHandler(context, repository);
        this.listSetsHandler = new ListSetsHandler(context, repository);
        this.listMetadataFormatsHandler = new DiffusionListMetadataFormatsHandler(context, repository);
        this.listRecordsHandler = new DiffusionListRecordsHandler(context, repository);
        this.listIdentifiersHandler = new DiffusionListIdentifiersHandler(context, repository);
        this.getRecordHandler = new DiffusionGetRecordHandler(context, repository);
        this.errorsHandler = new ErrorHandler();
    }

    public OAIPMH handle (Builder<OAIRequest> builder) throws OAIException {
        return handle(builder.build());
    }

    public OAIPMH handle (OAIRequest requestParameters) throws OAIException {
        log.log(Level.FINE, "Starting handling OAI request");
        Request request = new Request(repository.getConfiguration().getBaseUrl())
                .withVerbType(requestParameters.get(Verb))
                .withResumptionToken(requestParameters.get(ResumptionToken))
                .withIdentifier(requestParameters.get(Identifier))
                .withMetadataPrefix(requestParameters.get(MetadataPrefix))
                .withSet(requestParameters.get(Set))
                .withFrom(requestParameters.get(From))
                .withUntil(requestParameters.get(Until));

        OAIPMH response = new OAIPMH()
                .withRequest(request)
                .withResponseDate(dateProvider.now());
        try {
            OAICompiledRequest parameters = compileParameters(requestParameters);

            switch (request.getVerbType()) {
                case Identify:
                    response.withVerb(identifyHandler.handle(parameters));
                    break;
                case ListSets:
                    response.withVerb(listSetsHandler.handle(parameters));
                    break;
                case ListMetadataFormats:
                    response.withVerb(listMetadataFormatsHandler.handle(parameters));
                    break;
                case GetRecord:
                    response.withVerb(getRecordHandler.handle(parameters));
                    break;
                case ListIdentifiers:
                    response.withVerb(listIdentifiersHandler.handle(parameters));
                    break;
                case ListRecords:
                    response.withVerb(listRecordsHandler.handle(parameters));
                    break;
            }
        } catch (HandlerException e) {
            log.log(Level.FINE, e.getMessage(), e);
            response.withError(errorsHandler.handle(e));
        }

        return response;
    }

    private OAICompiledRequest compileParameters(OAIRequest requestParameters) throws IllegalVerbException, UnknownParameterException, BadArgumentException, DuplicateDefinitionException, BadResumptionToken {
        try {
            return requestParameters.compile();
        } catch (InvalidResumptionTokenException e) {
            throw new BadResumptionToken("The resumption token is invalid");
        }
    }
}
