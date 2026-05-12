package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.envers.RevisionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class RevisionDTO<T> {

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime dateTime;
    private Long revisionId;
    private String externalUserId;
    private RevisionType type;
    private String changedProps;

    private T entity;

    public void setExternalUserId(final String externalUserId) {
        if (externalUserId != null && !externalUserId.isEmpty())
            this.externalUserId = externalUserId;
    }

    public void setChangedProps(final String changedProps) {
        if (changedProps != null && !changedProps.isEmpty())
            this.changedProps = changedProps;
    }
}