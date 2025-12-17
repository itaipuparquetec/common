package br.org.itaipuparquetec.common.infrastructure.hibernate.function;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitForeignKeyNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;

/**
 * Hibernate custom {@link ImplicitNamingStrategyJpaCompliantImpl} to make the FK and UK names more readable for humans
 *
 * @author Emanuel Victor
 * @version 1.0.0
 * @since 2.0.0, 10/01/2020
 */
public class CustomNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

    /**
     * {@inheritDoc}
     *
     * @param source
     * @return
     */
    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        return this.toIdentifier("fk_" + source.getTableName().getCanonicalName()
                + "_" + source.getReferencedTableName().getCanonicalName(), source.getBuildingContext());
    }

    /**
     * {@inheritDoc}
     *
     * @param source
     * @return
     * @implNote this is not working due to this bug in Hibernate implementation
     * @see <a href="https://hibernate.atlassian.net/browse/HHH-11586">HHH-11586</>
     */
    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
        return this.toIdentifier("uk_" + source.getTableName().getCanonicalName(), source.getBuildingContext());
    }
}
