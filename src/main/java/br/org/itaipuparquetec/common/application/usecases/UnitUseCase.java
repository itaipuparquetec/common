package br.org.itaipuparquetec.common.application.usecases;

@FunctionalInterface
public interface UnitUseCase<Input> {
    void execute(Input input);
}
