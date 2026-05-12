package br.org.itaipuparquetec.common.infrastructure.audit;

import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class TrackingEntityRevisionListenerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mustSetUsernameFromSecurityContextOnNewRevision() {
        final var auth = new UsernamePasswordAuthenticationToken("john", null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        final var listener = new TrackingEntityRevisionListener();
        final var revision = new Revision();
        listener.newRevision(revision);

        assertThat(revision.getExternalUserId()).isEqualTo("john");
    }

    @Test
    void mustSetNullUsernameWhenSecurityContextHasNoAuthenticationOnNewRevision() {
        SecurityContextHolder.setContext(new SecurityContextImpl());

        final var listener = new TrackingEntityRevisionListener();
        final var revision = new Revision();
        listener.newRevision(revision);

        assertThat(revision.getExternalUserId()).isNull();
    }

    @Test
    void mustSetUsernameFromSecurityContextOnEntityChanged() {
        final var auth = new UsernamePasswordAuthenticationToken("jane", null, List.of());
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));

        final var listener = new TrackingEntityRevisionListener();
        final var revision = new Revision();
        listener.entityChanged(Object.class, "Object", 1L, RevisionType.ADD, revision);

        assertThat(revision.getExternalUserId()).isEqualTo("jane");
    }

    @Test
    void mustSetNullUsernameWhenSecurityContextHasNoAuthenticationOnEntityChanged() {
        SecurityContextHolder.setContext(new SecurityContextImpl());

        final var listener = new TrackingEntityRevisionListener();
        final var revision = new Revision();
        listener.entityChanged(Object.class, "Object", 1L, RevisionType.MOD, revision);

        assertThat(revision.getExternalUserId()).isNull();
    }
}
