package com.prpa.Shortz.repository.specification;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.repository.query.Search;
import com.prpa.Shortz.repository.query.SearchOperation;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static com.prpa.Shortz.repository.query.SearchOperation.LIKE;

public class ShortzUserSpecification implements Specification<ShortzUser> {

    private final Search search;

    public ShortzUserSpecification(Search search) {
        this.search = search;
    }

    @Override
    public Predicate toPredicate(Root<ShortzUser> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        String value = search.getValue();

        Optional<SearchOperation> searchOperation = search.getSearchOperation();
        if (searchOperation.isPresent() && searchOperation.get().equals(LIKE))
            value = "%" + value + "%";

        return search.getOperation().apply(criteriaBuilder, root.get(search.getField()), value);
    }
}
