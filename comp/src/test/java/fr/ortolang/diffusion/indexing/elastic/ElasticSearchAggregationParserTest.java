package fr.ortolang.diffusion.indexing.elastic;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.junit.Test;

public class ElasticSearchAggregationParserTest {

    private static final Logger LOGGER = Logger.getLogger(ElasticSearchAggregationParserTest.class.getName());

	@Test
	public void aggString() {
		String aggName = "corporaType";
		AggregationBuilder aggBuilder = ElasticSearchAggregationParser.parse(aggName);
		//TODO parse json
		assertNotNull(aggBuilder);
		LOGGER.log(Level.INFO, aggBuilder.toString());
	}

	@Test
	public void aggStringWithPath() {
		String aggName = "corporaType:corporaType.content";
		AggregationBuilder aggBuilder = ElasticSearchAggregationParser.parse(aggName);
		//TODO parse json
		assertNotNull(aggBuilder);
		LOGGER.log(Level.INFO, aggBuilder.toString());
	}

	@Test
	public void aggArray() {
		String aggName = "corporaFormats[]";
		AggregationBuilder aggBuilder = ElasticSearchAggregationParser.parse(aggName);
		//TODO parse json
		assertNotNull(aggBuilder);
		LOGGER.log(Level.INFO, aggBuilder.toString());
	}

	@Test
	public void aggArrayWithPath() {
		String aggName = "corporaFormats[]:corporaFormats.content";
		AggregationBuilder aggBuilder = ElasticSearchAggregationParser.parse(aggName);
		//TODO parse json
		assertNotNull(aggBuilder);
		LOGGER.log(Level.INFO, aggBuilder.toString());
	}

}
