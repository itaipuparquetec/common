package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.ApplicationContextAssert;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditTrailAutoConfigurationTest {
   private final ApplicationContextRunner runner = (ApplicationContextRunner)((ApplicationContextRunner)(new ApplicationContextRunner()).withConfiguration(AutoConfigurations.of(new Class[]{AuditTrailAutoConfiguration.class}))).withPropertyValues(new String[]{"spring.application.name=mirror"});

   AuditTrailAutoConfigurationTest() {
   }

   @Test
   void shouldRegisterTheAuditTrailBeansWhenEnabled() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.enabled=true"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditTrailAspect.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditEventSink.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).hasSingleBean(AuditProperties.class);
      });
   }

   @Test
   void shouldNotExposeTheMaskingObjectMapperNorItsModuleAsBeansWhenEnabled() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.enabled=true"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(ObjectMapper.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(Module.class);
      });
   }

   @Test
   void shouldNotRegisterTheAuditTrailBeansWhenDisabled() {
      ((ApplicationContextRunner)this.runner.withPropertyValues(new String[]{"audit.enabled=false"})).run(context -> {
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditTrailAspect.class);
         ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditEventSink.class);
      });
   }

   @Test
   void shouldNotRegisterTheAuditTrailBeansWhenThePropertyIsAbsent() {
      this.runner.run(context -> ((ApplicationContextAssert)Assertions.assertThat(context)).doesNotHaveBean(AuditTrailAspect.class));
   }
}
