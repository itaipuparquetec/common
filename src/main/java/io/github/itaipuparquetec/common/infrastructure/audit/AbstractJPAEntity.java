package io.github.itaipuparquetec.common.infrastructure.audit;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.envers.Audited;

import java.io.Serial;
import java.util.UUID;


/**
 *
 */
@Data
@MappedSuperclass
@Audited(withModifiedFlag = true)
public abstract class AbstractJPAEntity implements Entity<UUID> {

    @Serial
    private static final long serialVersionUID = -3875946542616104733L;

    /**
     *
     */
    @Id
    protected UUID id;

    /**
     *
     */
    public AbstractJPAEntity() {
    }

}
