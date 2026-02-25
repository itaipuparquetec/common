package br.org.itaipuparquetec.common.infrastructure.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TrackingEntityRevisionListenerTest {

    @Test
    void mustThrowNotImplementedExceptionWhenTryToCreateANewRevision() {
        final var listener = new TrackingEntityRevisionListener();

        final var exception = assertThrows(NotImplementedException.class, () -> listener.newRevision(null));

        assertThat(exception.getMessage()).isEqualTo("Not implemented yet");
    }

    @Test
    void mustThrowNotImplementedExceptionWhenTryToChangeEntity() {
        final var listener = new TrackingEntityRevisionListener();

        final var exception = assertThrows(NotImplementedException.class, () -> listener.entityChanged(
                null, null, null, null, null));

        assertThat(exception.getMessage()).isEqualTo("Not implemented yet");
    }
}
