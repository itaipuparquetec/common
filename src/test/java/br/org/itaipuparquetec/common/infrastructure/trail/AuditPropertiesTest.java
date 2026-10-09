package br.org.itaipuparquetec.common.infrastructure.trail;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class AuditPropertiesTest {
   AuditPropertiesTest() {
   }

   @ParameterizedTest
   @CsvSource({"PUBLIC, true, true, true, NONE", "PUBLIC, false, true, true, REDACT", "INTERNAL, true, true, true, NONE", "INTERNAL, true, false, true, REDACT", "CONFIDENTIAL, true, true, true, NONE", "CONFIDENTIAL, true, true, false, PSEUDONYMIZE"})
   void shouldResolveTheStrategyOfEachClassificationFromTheCleartextFlags(DataClassification classification, boolean publicData, boolean internal, boolean confidential, MaskingStrategy expected) {
      AuditProperties properties = propertiesWith(publicData, internal, confidential);
      MaskingStrategy strategy = properties.resolveStrategy(classification);
      Assertions.assertThat(strategy).isEqualTo(expected);
   }

   @ParameterizedTest
   @EnumSource(
      value = DataClassification.class,
      names = {"SENSITIVE"}
   )
   void shouldAlwaysRedactSensitiveDataEvenWhenEveryCleartextFlagIsOn(DataClassification classification) {
      AuditProperties properties = propertiesWith(true, true, true);
      MaskingStrategy strategy = properties.resolveStrategy(classification);
      Assertions.assertThat(strategy).isEqualTo(MaskingStrategy.REDACT);
   }

   @ParameterizedTest
   @CsvSource({"true, OUTBOX, true, true", "true, OUTBOX, false, false", "true, LOG, true, false", "false, OUTBOX, true, false"})
   void shouldMigrateTheOutboxSchemaOnlyWhenTheTrailIsEnabledWithTheOutboxSinkAndTheMigrationOn(boolean enabled, AuditSinkType sink, boolean migrateSchema, boolean expected) {
      AuditProperties properties = new AuditProperties(enabled, "salt", new AuditProperties.Cleartext(true, true, false), sink, false, 100, (String)null, new AuditProperties.Topics("events", "dlt"), new AuditProperties.Outbox(migrateSchema, 1, 1), AuditPropertiesFixture.relay());
      boolean migrates = properties.migratesOutboxSchema();
      Assertions.assertThat(migrates).isEqualTo(expected);
   }

   private static AuditProperties propertiesWith(boolean publicData, boolean internal, boolean confidential) {
      return AuditPropertiesFixture.withCleartext(publicData, internal, confidential);
   }
}
