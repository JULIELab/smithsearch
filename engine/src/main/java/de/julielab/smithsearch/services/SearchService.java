package de.julielab.smithsearch.services;

import de.julielab.smithsearch.data.SearchRequest;
import de.julielab.smithsearch.data.SearchResponse;

import java.io.IOException;

public interface SearchService {
    SearchResponse search(SearchRequest searchRequest) throws IOException;
}
