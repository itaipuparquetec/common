package io.github.itaipuparquetec.common.infrastructure.audit;

import java.io.Serializable;

/**
 * @version 1.0
 */
public interface Entity<ID extends Serializable> extends Serializable {
    /*-------------------------------------------------------------------
     * 		 				GETTERS AND SETTERS
     *-------------------------------------------------------------------*/

    /**
     * @return ID
     */
    ID getId();

    /**
     * @param id ID
     */
    void setId(ID id);
}
