package br.org.itaipuparquetec.common.infrastructure.rest;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {

    @Test
    void shouldThrowExceptionWhenPageIsNull() {
        assertThatThrownBy(() -> new PageResponse<>(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must not be null");
    }

    @Test
    void shouldReturnPageContent() {
        final var content = List.of("A", "B", "C");
        final var page = new PageImpl<>(content);

        final var response = new PageResponse<>(page);

        assertThat(response.getContent()).isEqualTo(content);
    }

    @Test
    void shouldCalculateTotalPagesCorrectly() {
        final var pageable = PageRequest.of(0, 5);
        final var page = new PageImpl<>(
                List.of("A", "B", "C", "D", "E"),
                pageable,
                12
        );

        final var response = new PageResponse<>(page);

        assertThat(response.getTotalPages()).isEqualTo(3);
    }

    @Test
    void shouldReturnOnePageWhenSizeIsZero() {
        final var page = new PageImpl<>(List.of());

        final var response = new PageResponse<>(page);

        assertThat(response.getTotalPages()).isEqualTo(1);
    }

    @Test
    void shouldReturnTotalElements() {
        final var page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(0, 2),
                10
        );

        final var response = new PageResponse<>(page);

        assertThat(response.getTotalElements()).isEqualTo(10);
    }

    @Test
    void shouldReturnHasNextTrueWhenNotLastPage() {
        final var page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(0, 2),
                5
        );

        final var response = new PageResponse<>(page);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.isLast()).isFalse();
    }

    @Test
    void shouldReturnHasNextFalseWhenLastPage() {
        final var page = new PageImpl<>(
                List.of("A"),
                PageRequest.of(2, 1),
                3
        );

        final var response = new PageResponse<>(page);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.isLast()).isTrue();
    }

    @Test
    void shouldReturnPageSize() {
        final var page = new PageImpl<>(
                List.of("A", "B"),
                PageRequest.of(0, 2),
                4
        );

        final var response = new PageResponse<>(page);

        assertThat(response.getSize()).isEqualTo(2);
    }

    @Test
    void equalsShouldReturnTrueForSamePage() {
        final var page = new PageImpl<>(List.of("A", "B"));

        final var first = new PageResponse<>(page);
        final var second = new PageResponse<>(page);

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second);
    }

    @Test
    void equalsShouldReturnFalseForDifferentObjects() {
        final var response = new PageResponse<>(new PageImpl<>(List.of("A")));

        assertThat(response).isNotEqualTo(null);
        assertThat(response).isNotEqualTo("not-a-page-response");
    }
}