package fr.ortolang.diffusion.api.oaipmh.dataprovider;

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

import fr.ortolang.diffusion.api.oaipmh.handlers.DiffusionGetRecordHandler;
import fr.ortolang.diffusion.api.oaipmh.handlers.DiffusionListIdentifiersHandler;
import fr.ortolang.diffusion.api.oaipmh.handlers.DiffusionListMetadataFormatsHandler;
import fr.ortolang.diffusion.api.oaipmh.handlers.DiffusionListRecordsHandler;

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
