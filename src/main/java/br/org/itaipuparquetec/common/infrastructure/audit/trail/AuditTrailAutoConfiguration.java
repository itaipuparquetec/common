package br.org.itaipuparquetec.common.infrastructure.audit.trail;

import br.org.itaipuparquetec.common.infrastructure.audit.trail.context.MdcTaskDecorator;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.context.TokenClaimsReader;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.serialization.ClassificationMaskingModule;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.serialization.Pseudonymizer;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.audit.trail.sink.Slf4jAuditEventSink;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration for the standalone audit trail instrumentation (starter).
 *
 * <p>Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}, so
 * consumers that do not use the broad {@code br.org.itaipuparquetec} component scan still get it. For consumers
 * that do use that scan, Spring dedupes the class (the scanned copy wins, the auto-config import is ignored), so
 * it is never wired twice.</p>
 *
 * <p>It only activates when {@code audit.enabled=true}, letting every microservice opt in explicitly. Following
 * the java-common starter convention, the collaborators are plain classes wired here as {@code @Bean}s.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AuditProperties.class)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AuditTrailAutoConfiguration {

    @Bean
    public Pseudonymizer auditPseudonymizer(final AuditProperties properties) {
        return new Pseudonymizer(properties.pseudonymizationSalt());
    }

    @Bean
    public ClassificationMaskingModule classificationMaskingModule(final AuditProperties properties,
                                                                   final Pseudonymizer auditPseudonymizer) {
        return new ClassificationMaskingModule(properties, auditPseudonymizer);
    }

    @Bean
    public ObjectMapper auditObjectMapper(final ClassificationMaskingModule classificationMaskingModule) {
        final var mapper = JsonMapper.builder().findAndAddModules().build();
        mapper.registerModule(classificationMaskingModule);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    @Bean
    public TokenClaimsReader tokenClaimsReader() {
        return new TokenClaimsReader();
    }

    @Bean
    public AuditEventSink auditEventSink(@Qualifier("auditObjectMapper") final ObjectMapper auditObjectMapper) {
        return new Slf4jAuditEventSink(auditObjectMapper);
    }

    @Bean
    public AuditTrailAspect auditTrailAspect(final AuditEventSink auditEventSink,
                                             final TokenClaimsReader tokenClaimsReader) {
        return new AuditTrailAspect(auditEventSink, tokenClaimsReader);
    }

    @Bean
    public MdcTaskDecorator auditMdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
