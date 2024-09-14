package com.prpa.Shortz.repository.specification;

import com.prpa.Shortz.model.ShortUrl;
import com.prpa.Shortz.repository.query.Search;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import static com.prpa.Shortz.repository.query.SearchOperation.*;

public class ShortUrlSpecification implements Specification<ShortUrl> {

    private final Search search;

    public ShortUrlSpecification(Search search) {
        this.search = search;
    }

    @Override
    public Predicate toPredicate(Root<ShortUrl> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        String value = search.value();
        if (search.searchOperation().equals(LIKE))
            value = "%" + value + "%";

        var function = search.searchOperation().getFunction();
        return function.apply(criteriaBuilder, root.get(search.field()), value);
    }
}
