package br.org.itaipuparquetec.common.application.usecases;

@FunctionalInterface
public interface UnitUseCase<I> {
    void execute(I input);
}
