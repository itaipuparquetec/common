package io.github.itaipuparquetec.common.infrastructure.audit;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serial;
import java.io.Serializable;


/**
 * @param <T>
 * @param <ID>
 */
@Data
@jakarta.persistence.Entity
@lombok.EqualsAndHashCode
@Table(name = Revision.TABLE_NAME)
@RevisionEntity(EntityTrackingRevisionListener.class)
public class Revision<T extends Entity<ID>, ID extends Serializable> implements Serializable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4193623660483050410L;

    /**
     *
     */
    public static final String TABLE_NAME = "REVISION";

    /**
     * id da {@link Revision}
     */
    @Id
    @RevisionNumber
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    /**
     * data da {@link Revision}
     */
    @RevisionTimestamp
    private long timestamp;

    /**
     * Username of the logged user {@link Revision}
     */
    private String username;


}
