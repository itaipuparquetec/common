package br.org.itaipuparquetec.common.domain.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResponse<T> {

    private int size;
    private boolean hasNext;
    private boolean isLast;
    private int pageNumber;
    private int totalPages;
    private List<T> content;
    private long totalElements;

    public PageResponse() {}

    public PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.totalPages = page.getSize() == 0 ? 1 : (int) Math.ceil((double) page.getTotalElements() / (double) page.getSize());
        this.totalElements = page.getTotalElements();
        this.pageNumber = page.getNumber();
        this.size = page.getSize();

        this.hasNext = this.pageNumber + 1 < this.getTotalPages();
        this.isLast = !this.hasNext;
    }

    @JsonProperty
    public List<T> getContent() {
        return content;
    }

    @JsonProperty
    public Integer getTotalPages() {
        return totalPages;
    }

    @JsonProperty
    public long getTotalElements() {
        return totalElements;
    }

    public boolean hasNext() {
        return this.hasNext;
    }

}
