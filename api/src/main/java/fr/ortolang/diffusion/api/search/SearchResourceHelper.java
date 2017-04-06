package fr.ortolang.diffusion.api.search;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import fr.ortolang.diffusion.search.SearchQuery;

public class SearchResourceHelper {

    private static final Logger LOGGER = Logger.getLogger(SearchResourceHelper.class.getName());

    public static SearchQuery executeQuery(HttpServletRequest request) {
    	return executeQuery(request, null, null);
    }
    
    public static SearchQuery executeQuery(HttpServletRequest request, String index) {
    	return executeQuery(request, index, null);
    }
    
    public static SearchQuery executeQuery(HttpServletRequest request, String index, String type) {
        SearchQuery query = new SearchQuery();
        if (index!=null) {
        	query.setIndex(index);
        }
        if (type!=null) {
        	query.setType(type);
        }
    	for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
            if ("type".equals(parameter.getKey())) {
            	query.setType(parameter.getValue()[0]);
            } else if ("index".equals(parameter.getKey())) {
            	query.setIndex(parameter.getValue()[0]);
            } else if ("size".equals(parameter.getKey())) {
            	try {
            		query.setSize(Integer.valueOf(parameter.getValue()[0]));
            	} catch (NumberFormatException e) {
            	}
            } else if ("includes".equals(parameter.getKey())) {
            	query.setIncludes(parameter.getValue());
            } else if ("excludes".equals(parameter.getKey())) {
            	query.setExcludes(parameter.getValue());
            } else if ("orderProp".equals(parameter.getKey())) {
            	query.setOrderProp(parameter.getValue()[0]);
            } else if ("orderDir".equals(parameter.getKey())) {
            	query.setOrderDir(parameter.getValue()[0]);
            } else if ("aggregations".equals(parameter.getKey())) {
            	LOGGER.log(Level.INFO, "Aggregations : " + parameter.getValue());
            	query.setAggregations(parameter.getValue());
            } else if (!"scope".equals(parameter.getKey())) {
                // Ignore scope param
            	query.addQuery(parameter.getKey(), parameter.getValue());
            }
    	}
    	return query;
    }
}
