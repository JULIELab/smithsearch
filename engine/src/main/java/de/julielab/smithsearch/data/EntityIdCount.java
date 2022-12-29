package de.julielab.smithsearch.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EntityIdCount {
    private String entityId;
    private long count;
}
