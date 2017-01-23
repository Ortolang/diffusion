package fr.ortolang.diffusion.indexing.elastic;

import static org.junit.Assert.assertNotNull;

import org.elasticsearch.index.query.QueryBuilder;
import org.junit.Test;

public class ElasticSearchSearchQueryParserTest {

	@Test
	public void queryArray() {
		QueryBuilder queryBuilder = ElasticSearchSearchQueryParser.parse("producers.id[]=atilf");
		assertNotNull(queryBuilder);
	}
}
