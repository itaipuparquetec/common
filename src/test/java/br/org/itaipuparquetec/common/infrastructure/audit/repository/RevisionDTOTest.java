package br.org.itaipuparquetec.common.infrastructure.audit.repository;

import org.hibernate.envers.RevisionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RevisionDTOTest {

    @Test
    void mustSetUsernameWhenNotNullOrEmpty() {
        final var dto = new RevisionDTO<String>();
        dto.setExternalUserId("user123");
        assertThat(dto.getExternalUserId()).isEqualTo("user123");
    }

    @Test
    void mustNotSetUsernameWhenNull() {
        final var dto = new RevisionDTO<String>();
        dto.setExternalUserId(null);
        assertThat(dto.getExternalUserId()).isNull();
    }

    @Test
    void mustNotSetUsernameWhenEmpty() {
        final var dto = new RevisionDTO<String>();
        dto.setExternalUserId("");
        assertThat(dto.getExternalUserId()).isNull();
    }

    @Test
    void mustSetChangedPropsWhenNotNullOrEmpty() {
        final var dto = new RevisionDTO<String>();
        dto.setChangedProps("field1,field2");
        assertThat(dto.getChangedProps()).isEqualTo("field1,field2");
    }

    @Test
    void mustNotSetChangedPropsWhenNull() {
        final var dto = new RevisionDTO<String>();
        dto.setChangedProps(null);
        assertThat(dto.getChangedProps()).isNull();
    }

    @Test
    void mustNotSetChangedPropsWhenEmpty() {
        final var dto = new RevisionDTO<String>();
        dto.setChangedProps("");
        assertThat(dto.getChangedProps()).isNull();
    }

    @Test
    void mustSetAllFieldsViaLombok() {
        final var dto = new RevisionDTO<String>();
        final var now = LocalDateTime.now();

        dto.dateTime = now;
        dto.setRevisionId(42L);
        dto.setType(RevisionType.ADD);
        dto.setEntity("payload");

        assertThat(dto.dateTime).isEqualTo(now);
        assertThat(dto.getRevisionId()).isEqualTo(42L);
        assertThat(dto.getType()).isEqualTo(RevisionType.ADD);
        assertThat(dto.getEntity()).isEqualTo("payload");
    }
}
