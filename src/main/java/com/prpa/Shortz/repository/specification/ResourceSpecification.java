package com.prpa.Shortz.repository.specification;

import com.prpa.Shortz.repository.query.SearchOperation;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.Objects;
import java.util.Optional;

import static com.prpa.Shortz.repository.query.SearchOperation.LIKE;

public class ResourceSpecification {

    public static <R> ResourceSpecificationBuilder<R> builder(Class<R> ignoredClazz) {
        return new ResourceSpecificationBuilder<>();
    }

    public static class ResourceSpecificationBuilder<T> {

        private Specification<T> specification;

        private ResourceSpecificationBuilder() {
            this.specification = null;
        }

        public ResourceSpecificationBuilder<T> and(String fieldsString, SearchOperation operation, String value) {
            if (value == null || value.isBlank()) return this;
            if (fieldsString == null || fieldsString.isBlank()) return this;
            Objects.requireNonNull(operation, "SearchOperation must not be null.");

            Specification<T> newSpecification = newSpecification(fieldsString, operation, value);

            if (this.specification == null) {
                this.specification = newSpecification;
                return this;
            }

            specification = specification.and(newSpecification);
            return this;
        }

        public ResourceSpecificationBuilder<T> or(String fieldsString, SearchOperation operation, String value) {
            if (value == null || value.isBlank()) return this;
            if (fieldsString == null || fieldsString.isBlank()) return this;
            Objects.requireNonNull(operation, "SearchOperation must not be null.");

            Specification<T> newSpecification = newSpecification(fieldsString, operation, value);

            if (this.specification == null) {
                this.specification = newSpecification;
                return this;
            }

            specification = specification.or(newSpecification);
            return this;
        }

        public Optional<Specification<T>> build() {
            return Optional.ofNullable(this.specification);
        }

        private Specification<T> newSpecification(String field, SearchOperation operation, String value) {
            return (root, query, criteriaBuilder) -> {
                String fieldValue = operation.equals(LIKE) ? "%" + value.trim().toLowerCase() + "%" : value.trim();

                String[] fields = field.trim().split("\\.");
                Path<String> path = root.get(fields[0]);

                for (int i = 1; i < fields.length; i++) {
                    path = path.get(fields[i]);
                }

                Expression<String> pathQuery = operation.equals(LIKE) ? criteriaBuilder.lower(path) : path;
                return operation.getFunction().apply(criteriaBuilder, pathQuery, fieldValue);
            };
        }
    }
}
