package de.julielab.smithsearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.julielab.smithsearch.data.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchServiceImplTest {

    @Test
    void search() throws Exception {
        final Map<String, List<String>> highlight = Map.of(SearchServiceImpl.FIELD_TEXT, List.of("test-highlight"));
        final Hit<SearchHit> hit = Hit.of(b -> b.index("test-index").id("42").highlight(highlight));

        final TotalHits totalHits = TotalHits.of(b -> b.value(1L).relation(TotalHitsRelation.Eq));

        final HitsMetadata<SearchHit> hitsMetadata = HitsMetadata.of(b -> b.hits(hit).total(totalHits));

        SearchResponse<SearchHit> searchResponse = SearchResponse.of(b ->
                b.hits(hitsMetadata)
                        .took(1L)
                        .timedOut(false)
                        .shards(ShardStatistics.of(s -> s.failed(0).total(1).successful(1)))
        );

        final ElasticsearchClient clientMock = mock(ElasticsearchClient.class);
        when(clientMock.search(any(SearchRequest.class), eq(SearchHit.class))).thenReturn(searchResponse);

        final SearchServiceImpl searchService = new SearchServiceImpl(clientMock);
        final de.julielab.smithsearch.data.SearchResponse smithResponse = searchService.search(new de.julielab.smithsearch.data.SearchRequest("test-query", 15, 10, true));

        assertEquals(1, smithResponse.getHits().size());
        assertEquals(1, smithResponse.getNumHits());
        assertEquals(TotalHitsRelation.Eq.name(), smithResponse.getNumHitsRelation());
        final SearchHit searchHit = smithResponse.getHits().get(0);
        assertEquals("42", searchHit.getDocId());
        assertEquals("test-highlight", searchHit.getText());
    }
}