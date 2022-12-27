package de.julielab.smithsearch.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Sorting {
    private String field;
    private Order order;
    public enum Order {asc, desc,}
}
