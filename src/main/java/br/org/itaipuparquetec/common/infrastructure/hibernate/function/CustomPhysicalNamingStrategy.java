package br.org.itaipuparquetec.common.infrastructure.hibernate.function;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class CustomPhysicalNamingStrategy implements PhysicalNamingStrategy {
    private static final CamelCaseToUnderscoresNamingStrategy STRATEGY = new CamelCaseToUnderscoresNamingStrategy();

    @Override
    public Identifier toPhysicalCatalogName(final Identifier identifier, final JdbcEnvironment jdbcEnvironment) {
        return STRATEGY.toPhysicalCatalogName(identifier, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalSchemaName(final Identifier identifier, final JdbcEnvironment jdbcEnvironment) {
        return STRATEGY.toPhysicalSchemaName(identifier, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalTableName(final Identifier identifier, final JdbcEnvironment jdbcEnvironment) {
        Identifier physicalTableName = STRATEGY.toPhysicalTableName(identifier, jdbcEnvironment);
        return new Identifier(physicalTableName.getText(), true);
    }

    @Override
    public Identifier toPhysicalSequenceName(final Identifier identifier, final JdbcEnvironment jdbcEnvironment) {
        return STRATEGY.toPhysicalSequenceName(identifier, jdbcEnvironment);
    }

    @Override
    public Identifier toPhysicalColumnName(final Identifier logicalName, final JdbcEnvironment jdbcEnvironment) {
        return STRATEGY.toPhysicalColumnName(logicalName, jdbcEnvironment);
    }
}
