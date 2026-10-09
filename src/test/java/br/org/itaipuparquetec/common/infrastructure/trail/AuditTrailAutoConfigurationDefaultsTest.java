package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.context.MdcTaskDecorator;
import br.org.itaipuparquetec.common.infrastructure.trail.context.TokenClaimsReader;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditTrailAutoConfigurationDefaultsTest {
   private final ApplicationContextRunner runner = (ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{AuditTrailAutoConfiguration.class}))).withPropertyValues(new String[]{"audit.enabled=true", "spring.application.name=mirror"});

   AuditTrailAutoConfigurationDefaultsTest() {
   }

   @Test
   void shouldBindTheDefaultCleartextFlags() {
      this.runner.run(context -> {
         AuditProperties properties = (AuditProperties)context.getBean(AuditProperties.class);
         Assertions.assertThat(properties.cleartext().publicData()).isTrue();
         Assertions.assertThat(properties.cleartext().internal()).isTrue();
         Assertions.assertThat(properties.cleartext().confidential()).isFalse();
      });
   }

   @Test
   void shouldBindTheCleartextFlagsFromTheEnvironment() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.cleartext.public=false", "audit.cleartext.confidential=true"})).run(context -> {
         AuditProperties properties = (AuditProperties)context.getBean(AuditProperties.class);
         Assertions.assertThat(properties.cleartext().publicData()).isFalse();
         Assertions.assertThat(properties.cleartext().confidential()).isTrue();
      });
   }

   @Test
   void shouldRegisterThePseudonymizerTheClaimsReaderAndTheTaskDecorator() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.pseudonymization-salt=pepper"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(Pseudonymizer.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(TokenClaimsReader.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(MdcTaskDecorator.class);
      });
   }
}
