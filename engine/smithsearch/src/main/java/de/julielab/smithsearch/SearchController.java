package de.julielab.smithsearch;

import de.julielab.smithsearch.data.SearchRequest;
import de.julielab.smithsearch.data.SearchResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {
    public static final String SEARCH_ENDPOINT = "/search";

    @PostMapping(value = SEARCH_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse search(SearchRequest request) {
        return new SearchResponse();
    }
}
