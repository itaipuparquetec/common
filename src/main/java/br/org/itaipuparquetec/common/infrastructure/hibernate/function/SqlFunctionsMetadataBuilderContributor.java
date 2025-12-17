package br.org.itaipuparquetec.common.infrastructure.hibernate.function;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.type.spi.TypeConfiguration;


public class SqlFunctionsMetadataBuilderContributor implements MetadataBuilderContributor {
    @Override
    public void contribute(MetadataBuilder metadataBuilder) {
        metadataBuilder.applySqlFunction(
                "FILTER",
                new FilterSqmFunctionDescriptor(new TypeConfiguration())
        );
    }
}
