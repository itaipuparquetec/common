package br.org.itaipuparquetec.common.infrastructure.audit;

import java.io.Serializable;

/**
 * @version 1.0
 */
public interface Entity<I extends Serializable> extends Serializable {
    /*-------------------------------------------------------------------
     * 		 				GETTERS AND SETTERS
     *-------------------------------------------------------------------*/

    /**
     * @return I
     */
    I getId();

    /**
     * @param id I
     */
    void setId(I id);
}
