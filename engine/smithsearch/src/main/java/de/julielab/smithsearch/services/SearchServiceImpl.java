package de.julielab.smithsearch.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringFlags;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import de.julielab.smithsearch.data.EntityIdCount;
import de.julielab.smithsearch.data.SearchHit;
import de.julielab.smithsearch.data.Sorting;
import org.springframework.context.annotation.Scope;
import org.springframework.data.elasticsearch.client.elc.Aggregation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope(value = "singleton")
public class SearchServiceImpl implements SearchService {

    public static final String FIELD_TEXT = "text";
    public static final String FIELD_DOCID = "doc_id";
    public static final String FIELD_ENTITYIDS = "entity_ids";
    private ElasticsearchClient client;

    public SearchServiceImpl(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public de.julielab.smithsearch.data.SearchResponse search(de.julielab.smithsearch.data.SearchRequest searchRequest) throws IOException {
        // The user can specify to a field to sort on and the order of sort.
        final List<SortOptions> sortOptions = searchRequest.getSorting().stream().map(s -> SortOptions.of(b -> b.field(f -> f.field(s.getField()).order(s.getOrder() == Sorting.Order.asc ? SortOrder.Asc : SortOrder.Desc)))).toList();
        // Whether to highlight matches in the text field with HTML tags.
        final Highlight highlight = searchRequest.isDoHighlighting() ? Highlight.of(b -> b.fields(FIELD_TEXT, HighlightField.of(f -> f.matchedFields(FIELD_TEXT)))) : null;
        TermsAggregation entityIdCountAgg = searchRequest.isDoFaceting() ? TermsAggregation.of(b -> b.field(FIELD_ENTITYIDS)) : null;

        final SimpleQueryStringQuery query = SimpleQueryStringQuery.of(b -> b
                .query(searchRequest.getQuery())
                .flags(SimpleQueryStringFlags.of(f -> f.multiple("ALL"))));
        final SearchRequest esSearchRequest = SearchRequest.of(srb -> {
            final SearchRequest.Builder builder = srb.storedFields(FIELD_TEXT)
                    .from(searchRequest.getFrom())
                    .size(searchRequest.getSize())
                    .source(b -> b.fetch(false))
                    .sort(sortOptions)
                    .highlight(highlight)
                    .query(b -> b.simpleQueryString(query));
            if (searchRequest.isDoFaceting())
                builder.aggregations(FIELD_ENTITYIDS, b -> b.terms(entityIdCountAgg));
            return builder;
        });


        final SearchResponse<SearchHit> response = client.search(esSearchRequest, SearchHit.class);
        final long numHits = response.hits().total().value();
        final TotalHitsRelation relation = response.hits().total().relation();
        final List<SearchHit> hits = response.hits().hits().stream().map(h -> new SearchHit(h.id(),
                        h.fields().get(FIELD_TEXT).to(String[].class)[0], h.highlight().get(FIELD_TEXT)))
                .collect(Collectors.toList());

        List<EntityIdCount> entityIdCounts = null;
        if (searchRequest.isDoFaceting())
            entityIdCounts = response.aggregations().get(FIELD_ENTITYIDS).sterms().buckets().array().stream().map(b -> new EntityIdCount(b.key().stringValue(), b.docCount())).toList();

        return new de.julielab.smithsearch.data.SearchResponse(hits, numHits, relation.name(), entityIdCounts);
    }
}
