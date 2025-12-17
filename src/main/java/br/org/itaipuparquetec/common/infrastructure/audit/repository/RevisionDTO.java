package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.envers.RevisionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO to exporting audit informations to client
 *
 * @param <T>
 */
@Data
public class RevisionDTO<T> {

    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    public LocalDateTime dateTime;
    private Long revisionId;
    private String username;
    private RevisionType type;
    private String changedProps;

    private T entity;

    /**
     * @param username String
     */
    public void setUsername(final String username) {
        if (username != null && !username.isEmpty())
            this.username = username;
    }

    /**
     * @param changedProps String
     */
    public void setChangedProps(final String changedProps) {
        if (changedProps != null && !changedProps.isEmpty())
            this.changedProps = changedProps;
    }
}