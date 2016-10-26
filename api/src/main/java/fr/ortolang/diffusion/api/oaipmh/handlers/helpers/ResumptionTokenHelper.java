package fr.ortolang.diffusion.api.oaipmh.handlers.helpers;

import com.lyncode.xoai.model.oaipmh.ResumptionToken;

import static com.google.common.base.Predicates.isNull;

public class ResumptionTokenHelper {
    private ResumptionToken.Value current;
    private long maxPerPage;
    private Long totalResults;

    public ResumptionTokenHelper(ResumptionToken.Value current, long maxPerPage) {
        this.current = current;
        this.maxPerPage = maxPerPage;
    }

    public ResumptionTokenHelper withTotalResults(long totalResults) {
        this.totalResults = totalResults;
        return this;
    }

    public ResumptionToken resolve (boolean hasMoreResults) {
        if (isInitialOffset() && !hasMoreResults) return null;
        else {
            if (hasMoreResults) {
                ResumptionToken.Value next = current.next(maxPerPage);
                return populate(new ResumptionToken(next));
            } else {
                ResumptionToken resumptionToken = new ResumptionToken();
                resumptionToken.withCursor(current.getOffset());
                if (totalResults != null)
                    resumptionToken.withCompleteListSize(totalResults);
                return resumptionToken;
            }
        }
    }

    private boolean isInitialOffset() {
        return isNull().apply(current.getOffset()) || current.getOffset() == 0;
    }

    private ResumptionToken populate(ResumptionToken resumptionToken) {
        if (totalResults != null)
            resumptionToken.withCompleteListSize(totalResults);
        resumptionToken.withCursor(current.getOffset());
        return resumptionToken;
    }
}

