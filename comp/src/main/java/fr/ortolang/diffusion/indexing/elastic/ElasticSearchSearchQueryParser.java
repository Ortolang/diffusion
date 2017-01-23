package fr.ortolang.diffusion.indexing.elastic;

import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class ElasticSearchSearchQueryParser {

	public static QueryBuilder parse(String query) {
		QueryBuilder queryBuilder = null;
		String parameter[] = query.split("=");
		if (parameter[0].endsWith("[]")) {
			String parameterKey = parameter[0].substring(0, parameter[0].length() - 2);
			String[] parameterKeyPart = parameterKey.split("\\.");
			String path = parameterKey;
			if (parameterKeyPart.length > 1) {
				path = parameterKeyPart[0];
			}
			queryBuilder = QueryBuilders.nestedQuery(path, QueryBuilders.termQuery(parameterKey, parameter[1]), ScoreMode.Avg);
		}
		return queryBuilder;
	}
}
