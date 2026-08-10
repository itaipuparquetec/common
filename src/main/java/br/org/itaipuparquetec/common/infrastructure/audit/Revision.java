package br.org.itaipuparquetec.common.infrastructure.audit;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serial;
import java.io.Serializable;

@Data
@jakarta.persistence.Entity
@lombok.EqualsAndHashCode
@Table(name = Revision.TABLE_NAME)
@RevisionEntity(TrackingEntityRevisionListener.class)
public class Revision implements Serializable {

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
    @GeneratedValue(generator = "revision_generator")
    @GenericGenerator(
            name = "revision_generator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @Parameter(name = "sequence_name", value = "revision_seq"),
                    @Parameter(name = "increment_size", value = "50"),
                    @Parameter(name = "optimizer", value = "pooled-lo")
            }
    )
    private long id;

    /**
     * data da {@link Revision}
     */
    @RevisionTimestamp
    private long timestamp;

    /**
     * ExternalUserId of the logged user {@link Revision}
     */
    private String externalUserId;


}
