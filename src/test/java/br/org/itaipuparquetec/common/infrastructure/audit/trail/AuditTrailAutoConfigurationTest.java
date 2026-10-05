package br.org.itaipuparquetec.common.infrastructure.audit.trail;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.sink.AuditEventSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTrailAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuditTrailAutoConfiguration.class));

    @Test
    void shouldRegisterTheAuditTrailBeansWhenEnabled() {
        runner.withPropertyValues("audit.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(AuditTrailAspect.class);
            assertThat(context).hasSingleBean(AuditEventSink.class);
            assertThat(context).hasSingleBean(AuditProperties.class);
        });
    }

    @Test
    void shouldNotRegisterTheAuditTrailBeansWhenDisabled() {
        runner.withPropertyValues("audit.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AuditTrailAspect.class);
            assertThat(context).doesNotHaveBean(AuditEventSink.class);
        });
    }

    @Test
    void shouldNotRegisterTheAuditTrailBeansWhenThePropertyIsAbsent() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuditTrailAspect.class));
    }
}
