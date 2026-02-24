package br.org.itaipuparquetec.common.application.usecases;

@FunctionalInterface
public interface UseCase<I, O> {
    O execute(I input);
}
