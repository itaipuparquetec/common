package br.org.itaipuparquetec.common.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

public record PageResponse<T>(Page<T> page) {

    public PageResponse {
        Assert.notNull(page, "Page must not be null");
    }

    @JsonProperty
    public List<T> getContent() {
        return page.getContent();
    }

    public int getTotalPages() {
        return this.page.getSize() == 0 ? 1 : (int) Math.ceil((double) this.page.getTotalElements() / (double) this.page.getSize());
    }

    public long getTotalElements() {
        return this.page.getTotalElements();
    }

    public boolean hasNext() {
        return this.page.getNumber() + 1 < this.getTotalPages();
    }

    public boolean isLast() {
        return !this.hasNext();
    }

    public long getSize() {
        return this.page.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PageResponse<?> that = (PageResponse<?>) o;
        return Objects.equals(page, that.page);
    }

}
