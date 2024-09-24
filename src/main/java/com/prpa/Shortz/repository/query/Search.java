package com.prpa.Shortz.repository.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;

import java.util.Optional;

@Getter
public class Search {

    private final String field;
    private final TriFunction<CriteriaBuilder, Path<?>, Object, Predicate> operation;
    private final String value;
    private SearchOperation searchOperation;

    @SuppressWarnings("unchecked")
    public Search(String field, SearchOperation operation, String value) {
        this(field, (criteriaBuilder, root, fieldValue) ->
                        operation.getFunction().apply(criteriaBuilder, (Expression<String>) root, fieldValue.toString())
                , value);
        this.searchOperation = operation;
    }

    public Search(String field, TriFunction<CriteriaBuilder, Path<?>, Object, Predicate> operation, String value) {
        this.field = field;
        this.operation = operation;
        this.value = value;
        this.searchOperation = null;
    }

    public Optional<SearchOperation> getSearchOperation() {
        return Optional.ofNullable(searchOperation);
    }
}
