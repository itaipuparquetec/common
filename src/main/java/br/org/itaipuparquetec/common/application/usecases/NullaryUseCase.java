package br.org.itaipuparquetec.common.application.usecases;

@FunctionalInterface
public interface NullaryUseCase<Output> {
    Output execute();
}
