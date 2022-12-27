package de.julielab.smithsearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlags;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.julielab.smithsearch.data.SearchHit;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class SearchServiceImpl implements SearchService {

    public static final String FIELD_TEXT = "text";
    private ElasticsearchClient client;

    public SearchServiceImpl(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public de.julielab.smithsearch.data.SearchResponse search(de.julielab.smithsearch.data.SearchRequest searchRequest) throws IOException {
        final SimpleQueryStringQuery query = SimpleQueryStringQuery.of(b -> b
                .query(searchRequest.getQuery())
                .flags(SimpleQueryStringFlags.of(f -> f.multiple("ALL"))));
        final SearchRequest esSearchRequest = SearchRequest.of(srb ->
                srb.storedFields(FIELD_TEXT)
                        .from(searchRequest.getFrom())
                        .size(searchRequest.getSize())
                        .source(b -> b.fetch(false))
                        .query(b -> b.simpleQueryString(query)));

        final SearchResponse<SearchHit> response = client.search(esSearchRequest, SearchHit.class);
        final long numHits = response.hits().total().value();
        final TotalHitsRelation relation = response.hits().total().relation();
        final List<SearchHit> hits = response.hits().hits().stream().map(h -> new SearchHit(h.id(),
                        h.highlight().containsKey(FIELD_TEXT) && !h.highlight().get(FIELD_TEXT).isEmpty() ? h.highlight().get(FIELD_TEXT).get(0) : null))
                .collect(Collectors.toList());

        return new de.julielab.smithsearch.data.SearchResponse(hits, numHits, relation.name());
    }
}
