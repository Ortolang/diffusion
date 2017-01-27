package fr.ortolang.diffusion.indexing.elastic;

import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

public class ElasticSearchSearchQueryParserTest {

    private static final Logger LOGGER = Logger.getLogger(ElasticSearchSearchQueryParserTest.class.getName());

	@Test
	public void queryString() {
		Map<String, String[]> queryMap = new HashMap<>();
		String[] values = {"free_use"};
		queryMap.put("statusOfUse.id", values);
		QueryBuilder queryBuilder = ElasticSearchSearchQueryParser.parse(queryMap);
		assertNotNull(queryBuilder);
		LOGGER.log(Level.INFO, queryBuilder.toString());
	}

	@Test
	public void querySuggesting() {
		Map<String, String[]> queryMap = new HashMap<>();
		String[] values = {"at"};
		queryMap.put("_all*", values);
		QueryBuilder queryBuilder = ElasticSearchSearchQueryParser.parse(queryMap);
		assertNotNull(queryBuilder);
		LOGGER.log(Level.INFO, queryBuilder.toString());
	}

	@Test
	public void queryMultiCriteria() {
		Map<String, String[]> queryMap = new HashMap<>();
		String[] statusOfUse_values = {"free_use"};
		String[] status_values = {"published"};
		queryMap.put("statusOfUse.id", statusOfUse_values);
		queryMap.put("status", status_values);
		QueryBuilder queryBuilder = ElasticSearchSearchQueryParser.parse(queryMap);
		assertNotNull(queryBuilder);
		LOGGER.log(Level.INFO, queryBuilder.toString());
	}
	
	@Test
	public void queryArray() {
		Map<String, String[]> queryMap = new HashMap<>();
		String[] producersId_value = {"atilf"};
		queryMap.put("producers.id[]", producersId_value);
		QueryBuilder queryBuilder = ElasticSearchSearchQueryParser.parse(queryMap);
		assertNotNull(queryBuilder);
		LOGGER.log(Level.INFO, queryBuilder.toString());
	}
	
}
