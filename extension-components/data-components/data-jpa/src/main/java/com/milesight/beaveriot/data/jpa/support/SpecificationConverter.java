package com.milesight.beaveriot.data.jpa.support;

import com.milesight.beaveriot.base.exception.DataAccessException;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.data.filterable.SearchFilter;
import com.milesight.beaveriot.data.filterable.condition.CompareCondition;
import com.milesight.beaveriot.data.filterable.condition.CompositeCondition;
import com.milesight.beaveriot.data.filterable.condition.Condition;
import com.milesight.beaveriot.data.filterable.enums.BooleanOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author leon
 */
public class SpecificationConverter {

    private SpecificationConverter() {
    }

    public static <T> Specification<T> toSpecification(Consumer<Filterable> consumer) {
        SearchFilter nextQuerySpecs = new SearchFilter(BooleanOperator.AND, new ArrayList<>());
        consumer.accept(nextQuerySpecs);
        return SpecificationConverter.toSpecification(nextQuerySpecs);
    }

    public static <T> Specification<T> toSpecification(SearchFilter searchFilter) {
        return  (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = toPredicateList(root, query, criteriaBuilder, searchFilter.getConditions(), new ArrayList<>());
            return query.where(predicates.toArray(new Predicate[0])).getRestriction();
        };
    }

    private static <T> List<Predicate> toPredicateList(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder, List<Condition> conditions, List<Predicate> predicates) {
        for (Condition condition : conditions) {
            if (condition instanceof CompareCondition compareCondition) {
                buildCompareCondition(compareCondition, root, predicates, criteriaBuilder);
            }else if(condition instanceof CompositeCondition compositeCondition) {
                buildCompositeCondition(compositeCondition, root, query, criteriaBuilder, predicates );
            }else{
                throw new DataAccessException("Unsupported condition type");
            }
        }
        return predicates;
    }

    private static <T> void buildCompositeCondition(CompositeCondition compositeCondition, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder, List<Predicate> predicates) {
        List<Predicate> compositePredicates = toPredicateList(root, query, criteriaBuilder, compositeCondition.getConditions(), new ArrayList<>());
        if(!CollectionUtils.isEmpty(compositePredicates)) {
            if(compositeCondition.getBooleanOperator() == BooleanOperator.AND) {
                predicates.add(criteriaBuilder.and(compositePredicates.toArray(new Predicate[0])));
            }else{
                predicates.add(criteriaBuilder.or(compositePredicates.toArray(new Predicate[0])));
            }
        }
    }

    private static <T> void buildCompareCondition(CompareCondition compareCondition,Root<T> root, List<Predicate> predicates, CriteriaBuilder criteriaBuilder) {
        switch (compareCondition.getSearchOperator()) {
            case EQ:
                predicates.add(criteriaBuilder.equal(root.get(compareCondition.getName()), compareCondition.getValue()));
                break;
            case NE:
                predicates.add(criteriaBuilder.notEqual(root.get(compareCondition.getName()), compareCondition.getValue()));
                break;
            case GT:
                predicates.add(criteriaBuilder.greaterThan(root.get(compareCondition.getName()), (Comparable) compareCondition.getValue()));
                break;
            case GE:
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get(compareCondition.getName()), (Comparable) compareCondition.getValue()));
                break;
            case LT:
                predicates.add(criteriaBuilder.lessThan(root.get(compareCondition.getName()), (Comparable) compareCondition.getValue()));
                break;
            case LE:
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get(compareCondition.getName()), (Comparable) compareCondition.getValue()));
                break;
            case CASE_IGNORE_LIKE:
                predicates.add(criteriaBuilder.like(criteriaBuilder.upper(root.get(compareCondition.getName())), "%" + compareCondition.getValue().toString().toUpperCase() + "%"));
                break;
            case CASE_IGNORE_NOT_LIKE:
                predicates.add(criteriaBuilder.notLike(criteriaBuilder.upper(root.get(compareCondition.getName())), "%" + compareCondition.getValue().toString().toUpperCase() + "%"));
                break;
            case CASE_IGNORE_STARTS_WITH:
                predicates.add(criteriaBuilder.like(criteriaBuilder.upper(root.get(compareCondition.getName())), compareCondition.getValue().toString().toUpperCase() + "%"));
                break;
            case CASE_IGNORE_ENDS_WITH:
                predicates.add(criteriaBuilder.like(criteriaBuilder.upper(root.get(compareCondition.getName())), "%" + compareCondition.getValue().toString().toUpperCase()));
                break;
            case LIKE:
                predicates.add(criteriaBuilder.like(root.get(compareCondition.getName()), "%" + compareCondition.getValue() + "%"));
                break;
            case NOT_LIKE:
                predicates.add(criteriaBuilder.notLike(root.get(compareCondition.getName()), "%" + compareCondition.getValue() + "%"));
                break;
            case STARTS_WITH:
                predicates.add(criteriaBuilder.like(root.get(compareCondition.getName()), compareCondition.getValue() + "%"));
                break;
            case ENDS_WITH:
                predicates.add(criteriaBuilder.like(root.get(compareCondition.getName()), "%" + compareCondition.getValue()));
                break;
            case BETWEEN:
                Pair<Object,Object> betweenValues = (Pair<Object,Object>) compareCondition.getValue();
                predicates.add(criteriaBuilder.between(root.get(compareCondition.getName()), (Comparable<Object>) betweenValues.getFirst(), (Comparable<Object>) betweenValues.getSecond()));
                break;
            case IS_NULL:
                predicates.add(criteriaBuilder.isNull(root.get(compareCondition.getName())));
                break;
            case IS_NOT_NULL:
                predicates.add(criteriaBuilder.isNotNull(root.get(compareCondition.getName())));
                break;
            case IN:
                predicates.add(root.get(compareCondition.getName()).in((Object[]) compareCondition.getValue()));
                break;
            case NOT_IN:
                predicates.add(criteriaBuilder.not(root.get(compareCondition.getName()).in((Object[]) compareCondition.getValue())));
                break;
            default:
                throw new IllegalArgumentException("Unsupported search operator");
        }
    }
}
