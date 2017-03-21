package fr.ortolang.diffusion.indexing.elastic;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

public class ElasticSearchAggregationParser {

	public static AggregationBuilder parse(String aggName) {
		if (aggName.endsWith("[]")) {
			String parameterKey = aggName.substring(0, aggName.length() - 2);
			return AggregationBuilders.nested(parameterKey, parameterKey).subAggregation(AggregationBuilders.terms("content").field(parameterKey + ".content"));
		} else {			
			return AggregationBuilders.terms(aggName).field(aggName + ".content");
		}
	}

}
