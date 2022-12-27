package de.julielab.smithsearch.data;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@RequiredArgsConstructor
public class SearchRequest {
    @NonNull
    private String query;
    @NonNull
    private int from;
    @NonNull
    private int size;
    private List<Sorting> sorting = Collections.emptyList();
}
