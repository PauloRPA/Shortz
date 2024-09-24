package com.prpa.Shortz.repository.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.Getter;

@Getter
public enum SearchOperation {

    EQUALS(":", CriteriaBuilder::equal),
    LIKE("::", CriteriaBuilder::like),
    GREATER(">", CriteriaBuilder::greaterThan),
    LESS("<", CriteriaBuilder::lessThan),
    GREATER_EQUAL(">:", CriteriaBuilder::greaterThanOrEqualTo),
    LESSER_EQUAL("<:", CriteriaBuilder::lessThanOrEqualTo);

    private final String token;
    private final TriFunction<CriteriaBuilder, Expression<String>, String, Predicate> function;

    SearchOperation(String strToken,
                    TriFunction<CriteriaBuilder, Expression<String>, String, Predicate> func) {
        this.token = strToken;
        this.function = func;
    }


}
