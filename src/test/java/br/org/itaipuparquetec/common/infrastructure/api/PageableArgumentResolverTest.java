package br.org.itaipuparquetec.common.infrastructure.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PageableArgumentResolverTest {

    private PageableArgumentResolver resolver;

    @Mock
    private MethodParameter methodParameter;

    @Mock
    private HttpRequestValues.Builder requestValues;

    @BeforeEach
    void setUp() {
        resolver = new PageableArgumentResolver();
    }

    @Nested
    @DisplayName("when the argument is not Pageable")
    class ArgumentNotPageable {

        @Test
        @DisplayName("returns false without adding parameters")
        void returnsFalseWithoutAddingParameters() {
            boolean result = resolver.resolve("any string", methodParameter, requestValues);

            assertThat(result).isFalse();
            verifyNoInteractions(requestValues);
        }

        @Test
        @DisplayName("returns false for null argument")
        void returnsFalseForNullArgument() {
            boolean result = resolver.resolve(null, methodParameter, requestValues);

            assertThat(result).isFalse();
            verifyNoInteractions(requestValues);
        }
    }

    @Nested
    @DisplayName("when Pageable is paged without Sort")
    class PageablePagedWithoutSort {

        @Test
        @DisplayName("returns true and adds page and size")
        void addsPageAndSize() {
            Pageable pageable = PageRequest.of(0, 10);

            boolean result = resolver.resolve(pageable, methodParameter, requestValues);

            assertThat(result).isTrue();
            verify(requestValues).addRequestParameter("page", "0");
            verify(requestValues).addRequestParameter("size", "10");
            verifyNoMoreInteractions(requestValues);
        }

        @Test
        @DisplayName("uses the correct page number when not the first")
        void usesCorrectPageNumber() {
            Pageable pageable = PageRequest.of(3, 20);

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues).addRequestParameter("page", "3");
            verify(requestValues).addRequestParameter("size", "20");
        }
    }

    @Nested
    @DisplayName("when Pageable contains Sort")
    class PageableWithSort {

        @Test
        @DisplayName("adds sort with ascending direction")
        void addsAscendingSort() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.asc("name")));

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues).addRequestParameter("sort", "name,asc");
        }

        @Test
        @DisplayName("adds sort with descending direction")
        void addsDescendingSort() {
            Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Order.desc("createdAt")));

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues).addRequestParameter("sort", "createdAt,desc");
        }

        @Test
        @DisplayName("adds a sort parameter per property when there are multiple orders")
        void addsSortForEachProperty() {
            Pageable pageable = PageRequest.of(0, 10,
                    Sort.by(Sort.Order.asc("name"), Sort.Order.desc("createdAt")));

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues).addRequestParameter("sort", "name,asc");
            verify(requestValues).addRequestParameter("sort", "createdAt,desc");
        }

        @Test
        @DisplayName("adds page, size and sort together")
        void addsAllParameters() {
            Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Order.asc("name")));

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues).addRequestParameter("page", "2");
            verify(requestValues).addRequestParameter("size", "5");
            verify(requestValues).addRequestParameter("sort", "name,asc");
            verifyNoMoreInteractions(requestValues);
        }
    }

    @Nested
    @DisplayName("when Pageable is unpaged")
    class PageableUnpaged {

        @Test
        @DisplayName("does not add page or size")
        void doesNotAddPageOrSize() {
            Pageable pageable = Pageable.unpaged();

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues, never()).addRequestParameter(eq("page"), any());
            verify(requestValues, never()).addRequestParameter(eq("size"), any());
        }

        @Test
        @DisplayName("returns true even without adding pagination parameters")
        void returnsTrueEvenWithoutParameters() {
            Pageable pageable = Pageable.unpaged();

            boolean result = resolver.resolve(pageable, methodParameter, requestValues);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("adds sort even when unpaged")
        void addsSortWhenUnpaged() {
            Pageable pageable = Pageable.unpaged(Sort.by(Sort.Order.desc("name")));

            resolver.resolve(pageable, methodParameter, requestValues);

            verify(requestValues, never()).addRequestParameter(eq("page"), any());
            verify(requestValues, never()).addRequestParameter(eq("size"), any());
            verify(requestValues).addRequestParameter("sort", "name,desc");
        }
    }
}
