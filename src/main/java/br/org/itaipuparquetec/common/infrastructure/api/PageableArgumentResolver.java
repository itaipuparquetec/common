package br.org.itaipuparquetec.common.infrastructure.api;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.web.service.invoker.HttpRequestValues;
import org.springframework.web.service.invoker.HttpServiceArgumentResolver;

public class PageableArgumentResolver implements HttpServiceArgumentResolver {

    @Override
    public boolean resolve(Object argument, @NonNull MethodParameter parameter,
                           @NonNull HttpRequestValues.Builder requestValues) {
        if (!(argument instanceof Pageable pageable)) {
            return false;
        }

        if (pageable.isPaged()) {
            requestValues.addRequestParameter("page", String.valueOf(pageable.getPageNumber()));
            requestValues.addRequestParameter("size", String.valueOf(pageable.getPageSize()));
        }

        for (Sort.Order order : pageable.getSort()) {
            String direction = order.getDirection().name().toLowerCase();
            requestValues.addRequestParameter("sort", order.getProperty() + "," + direction);
        }

        return true;
    }
}