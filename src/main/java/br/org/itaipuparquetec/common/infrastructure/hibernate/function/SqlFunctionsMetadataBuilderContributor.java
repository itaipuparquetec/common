package br.org.itaipuparquetec.common.infrastructure.hibernate.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.spi.TypeConfiguration;


public class SqlFunctionsMetadataBuilderContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();

        functionContributions.getFunctionRegistry().register(
                "FILTER",
                new FilterSqmFunctionDescriptor(typeConfiguration)
        );
    }
}
