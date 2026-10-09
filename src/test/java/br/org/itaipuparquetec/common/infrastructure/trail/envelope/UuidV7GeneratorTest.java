package br.org.itaipuparquetec.common.infrastructure.trail.envelope;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Random;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class UuidV7GeneratorTest {
   private static final Instant NOW = Instant.parse("2026-10-07T13:45:12.345Z");
   private final UuidV7Generator generator;

   UuidV7GeneratorTest() {
      this.generator = new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC), new Random(42L));
   }

   @Test
   void shouldGenerateAVersion7Rfc4122Uuid() {
      UUID uuid = this.generator.next();
      Assertions.assertThat(uuid.version()).isEqualTo(7);
      Assertions.assertThat(uuid.variant()).isEqualTo(2);
      Assertions.assertThat(uuid.toString()).matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
   }

   @Test
   void shouldCarryTheUnixMillisecondsInTheFirst48Bits() {
      UUID uuid = this.generator.next();
      Assertions.assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(NOW.toEpochMilli());
   }

   @Test
   void shouldGenerateDifferentIdsWithinTheSameMillisecond() {
      UUID first = this.generator.next();
      UUID second = this.generator.next();
      Assertions.assertThat(first).isNotEqualTo(second);
   }

   @Test
   void shouldGenerateIdsOrderedByTimeAcrossMilliseconds() {
      UUID earlier = (new UuidV7Generator(Clock.fixed(NOW, ZoneOffset.UTC), new Random(1L))).next();
      UUID later = (new UuidV7Generator(Clock.fixed(NOW.plusMillis(5L), ZoneOffset.UTC), new Random(1L))).next();
      Assertions.assertThat(earlier.toString()).isLessThan(later.toString());
   }
}
