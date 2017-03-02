package fr.ortolang.diffusion.indexing.elastic;

import java.util.Map;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class ElasticSearchQueryParser {

	/**
	 * Parses a query.
	 * 
	 * @param queryMap
	 *            value of the map must be either a String or an List of String
	 *            or a Long
	 * @return the query builder
	 */
	public static QueryBuilder parse(Map<String, String[]> queryMap) {
		BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
		
		for (Map.Entry<String, String[]> entry : queryMap.entrySet()) {
			String keyQuery = entry.getKey();
			String[] valuesQuery = entry.getValue();
			// String parameter[] = query.split("=");
			
//			for (String valueQuery : valuesQuery) {
				if (keyQuery.endsWith("[]")) {
					String parameterKey = keyQuery.substring(0, keyQuery.length() - 2);
					String[] parameterKeyPart = parameterKey.split("\\.");
					String path = parameterKey;
					if (parameterKeyPart.length > 1) {
						path = parameterKeyPart[0];
					}
					if (valuesQuery.length==1) {
						queryBuilder.must(QueryBuilders.nestedQuery(path, QueryBuilders.termQuery(parameterKey, valuesQuery[0]),ScoreMode.Avg));
					} else {
						queryBuilder.must(QueryBuilders.nestedQuery(path, QueryBuilders.termsQuery(parameterKey, valuesQuery),ScoreMode.Avg));
					}
				} else if (keyQuery.endsWith("*")) {
					String parameterKey = keyQuery.substring(0, keyQuery.length() - 1);
					if (valuesQuery.length==1) {					
						queryBuilder.must(QueryBuilders.matchPhrasePrefixQuery(parameterKey, valuesQuery[0]));
					}
				} else {
					if (valuesQuery.length==1) {
						queryBuilder.must(QueryBuilders.termQuery(keyQuery, valuesQuery[0]));
					} else {
						queryBuilder.must(QueryBuilders.termsQuery(keyQuery, valuesQuery));
					}
				}
//			}
		}
		return queryBuilder;
	}
}
