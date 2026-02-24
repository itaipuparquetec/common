package br.org.itaipuparquetec.common.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(@JsonProperty @JsonIgnore Page<T> page) {

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
}
