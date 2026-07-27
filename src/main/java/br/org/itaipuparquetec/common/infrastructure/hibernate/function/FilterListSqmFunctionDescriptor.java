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

public class FilterListSqmFunctionDescriptor extends AbstractSqmSelfRenderingFunctionDescriptor {

    public FilterListSqmFunctionDescriptor(final TypeConfiguration typeConfiguration) {
        super(
                "FILTERER",
                FunctionKind.NORMAL,
                StandardArgumentsValidators.min(2),
                StandardFunctionReturnTypeResolvers.invariant(
                        typeConfiguration.getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN)
                ),
                null
        );
    }

    @Override
    public void render(final SqlAppender sqlAppender, final List<? extends SqlAstNode> sqlAstArguments, final ReturnableType<?> returnType, final SqlAstTranslator<?> walker) {

        sqlAppender.appendSql("(");

        sqlAppender.appendSql("CASE WHEN ");
        sqlAstArguments.getFirst().accept(walker);
        sqlAppender.appendSql(" IS NULL OR cardinality(");
        sqlAstArguments.getFirst().accept(walker);
        sqlAppender.appendSql(") = 0 THEN TRUE ELSE ");

        sqlAppender.appendSql("EXISTS (SELECT 1 FROM unnest(");
        sqlAstArguments.getFirst().accept(walker);
        sqlAppender.appendSql(") f WHERE filter(CAST(f AS VARCHAR)");

        for (int i = 1; i < sqlAstArguments.size(); i++) {
            sqlAppender.appendSql(", ");
            sqlAstArguments.get(i).accept(walker);
        }

        sqlAppender.appendSql(")) END)");

    }
}
