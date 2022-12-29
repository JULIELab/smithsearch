package de.julielab.smithsearch;

import de.julielab.smithsearch.data.SearchRequest;
import de.julielab.smithsearch.data.SearchResponse;
import de.julielab.smithsearch.services.SearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class SearchController {
    public static final String SEARCH_ENDPOINT = "/search";
    private SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping(value = SEARCH_ENDPOINT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SearchResponse search(@RequestBody SearchRequest request) throws IOException {
        return searchService.search(request);
    }
}
