package fr.ortolang.diffusion.indexing.elastic;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

public class ElasticSearchAggregationParser {

	public static AggregationBuilder parse(String aggName) {
		String aggPath = aggName;
		String aggField = aggName;
		String[] aggNameSplit = aggName.split(":");
		if (aggNameSplit.length>1) {
			aggPath = aggNameSplit[0];
			aggField = aggNameSplit[1];
		}
		if (aggPath.endsWith("[]")) {
			String parameterKey = aggPath.substring(0, aggPath.length() - 2);
			if (aggField.endsWith("[]")) {
				aggField = aggField.substring(0, aggField.length() - 2);
			}
			return AggregationBuilders.nested(parameterKey, parameterKey).subAggregation(AggregationBuilders.terms("content").field(aggField));
		} else {			
			return AggregationBuilders.terms(aggPath).field(aggField);
		}
	}

}
