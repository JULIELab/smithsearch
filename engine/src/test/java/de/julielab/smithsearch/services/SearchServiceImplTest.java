package de.julielab.smithsearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch._types.aggregations.Buckets;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.julielab.smithsearch.data.EntityIdCount;
import de.julielab.smithsearch.data.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceImplTest {

    @Test
    void search() throws Exception {
        final Map<String, List<String>> highlight = Map.of(SearchServiceImpl.FIELD_TEXT, List.of("test-highlight"));
        // Set up a search hit with document ID, highlights and the actual document text
        final Hit<SearchHit> hit = Hit.of(b -> b.index("test-index").id("42").highlight(highlight)
                // A real ElasticSearch instance returns arrays of strings even if there is only a single value.
                .fields(SearchServiceImpl.FIELD_TEXT, JsonData.of(new String[]{"test-text"}, new JacksonJsonpMapper())));

        final TotalHits totalHits = TotalHits.of(b -> b.value(1L).relation(TotalHitsRelation.Eq));

        final HitsMetadata<SearchHit> hitsMetadata = HitsMetadata.of(b -> b.hits(hit).total(totalHits));

        SearchResponse<SearchHit> searchResponse = SearchResponse.of(b ->
                b.hits(hitsMetadata)
                        .took(1L)
                        .timedOut(false)
                        .shards(ShardStatistics.of(s -> s.failed(0).total(1).successful(1)))
                        .aggregations(SearchServiceImpl.FIELD_ENTITYIDS,
                                a -> a.sterms(StringTermsAggregate.of(
                                        s -> s.buckets(Buckets.of(
                                                bu -> bu.array(List.of(StringTermsBucket.of(
                                                        str -> str.key("id1").docCount(13L)))))))))
        );

        final ElasticsearchClient clientMock = mock(ElasticsearchClient.class);
        when(clientMock.search(any(SearchRequest.class), eq(SearchHit.class))).thenReturn(searchResponse);

        final SearchServiceImpl searchService = new SearchServiceImpl(clientMock);
        final de.julielab.smithsearch.data.SearchResponse smithResponse = searchService.search(new de.julielab.smithsearch.data.SearchRequest("test-query", 15, 10, true, true, Collections.emptyList()));

        assertEquals(1, smithResponse.getHits().size());
        assertEquals(1, smithResponse.getNumHits());
        assertEquals(TotalHitsRelation.Eq.name(), smithResponse.getNumHitsRelation());
        final SearchHit searchHit = smithResponse.getHits().get(0);
        assertEquals("42", searchHit.getDocId());
        assertEquals("test-text", searchHit.getText());
        assertEquals("test-highlight", searchHit.getHighlights().get(0));
        assertNotNull(smithResponse.getEntityIdCounts());
        assertEquals(1, smithResponse.getEntityIdCounts().size());
        final EntityIdCount entityIdCount = smithResponse.getEntityIdCounts().get(0);
        assertEquals("id1", entityIdCount.getEntityId());
        assertEquals(13L, entityIdCount.getCount());
    }
}