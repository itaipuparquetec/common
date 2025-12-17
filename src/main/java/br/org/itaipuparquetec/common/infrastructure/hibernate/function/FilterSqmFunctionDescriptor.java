package br.org.itaipuparquetec.common.infrastructure.hibernate.function;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

public class FilterSqmFunctionDescriptor extends AbstractSqmSelfRenderingFunctionDescriptor {

    public FilterSqmFunctionDescriptor(TypeConfiguration typeConfiguration) {
        super(
                "FILTER",                    // Function name
                FunctionKind.NORMAL,             // Regular SQL function
                StandardArgumentsValidators.min(2),  // Ao menos 2 argumentos
                StandardFunctionReturnTypeResolvers.invariant(
                        typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
                ),
                null
        );
    }

    @Override
    public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> sqlAstArguments, ReturnableType<?> returnType, SqlAstTranslator<?> walker) {
        // Lidar com o caso onde o filtro é nulo separadamente
        sqlAppender.appendSql("(");

        // Primeiro verificamos se o filtro é nulo ou vazio
        sqlAppender.appendSql("CASE WHEN ");
        sqlAstArguments.get(0).accept(walker);
        sqlAppender.appendSql(" IS NULL OR TRIM(CAST(");
        sqlAstArguments.get(0).accept(walker);
        sqlAppender.appendSql(" AS VARCHAR)) = '' THEN TRUE ELSE ");

        // Se não for nulo ou vazio, usamos a função filter
        sqlAppender.appendSql("filter(CAST(");
        sqlAstArguments.get(0).accept(walker);
        sqlAppender.appendSql(" AS VARCHAR)");

        // Argumentos restantes: os campos a serem pesquisados
        for (int i = 1; i < sqlAstArguments.size(); i++) {
            sqlAppender.appendSql(", ");
            sqlAstArguments.get(i).accept(walker);
        }

        // Fechamos a chamada da função e o CASE
        sqlAppender.appendSql(") END)");
    }
}