package de.julielab.smithsearch.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    private List<SearchHit> hits;
    private long numHits;
    private String numHitsRelation;
    List<EntityIdCount> entityIdCounts;
}
