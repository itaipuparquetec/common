package br.org.itaipuparquetec.common.infrastructure.trail;

import br.org.itaipuparquetec.common.application.services.TenantIdentifierService;
import br.org.itaipuparquetec.common.infrastructure.trail.context.MdcTaskDecorator;
import br.org.itaipuparquetec.common.infrastructure.trail.context.TokenClaimsReader;
import br.org.itaipuparquetec.common.infrastructure.trail.envelope.UuidV7Generator;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditActorResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.AuditTenantResolver;
import br.org.itaipuparquetec.common.infrastructure.trail.identity.SourceIdentity;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.AuditMappers;
import br.org.itaipuparquetec.common.infrastructure.trail.serialization.Pseudonymizer;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.AuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.sink.Slf4jAuditEventSink;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.MicrometerUseCaseTracing;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.StandaloneUseCaseTracing;
import br.org.itaipuparquetec.common.infrastructure.trail.tracing.UseCaseTracing;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Auto-configuration for the standalone audit trail instrumentation (starter).
 *
 * <p>Registered in {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}, so
 * consumers that do not use the broad {@code br.org.itaipuparquetec} component scan still get it. For consumers
 * that do use that scan, Spring dedupes the class (the scanned copy wins, the auto-config import is ignored), so
 * it is never wired twice.</p>
 *
 * <p>It only activates when {@code audit.enabled=true}, letting every microservice opt in explicitly. The sink is
 * chosen by {@code audit.sink}: {@code log} (default, this class) or {@code outbox}
 * ({@link AuditOutboxAutoConfiguration}). The two are mutually exclusive by property, never by bean ordering, so
 * they behave the same when component-scanned.</p>
 *
 * <p>The masking {@code ObjectMapper} and its module are deliberately not beans (see {@link AuditMappers}).</p>
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
    public TokenClaimsReader tokenClaimsReader() {
        return new TokenClaimsReader();
    }

    @Bean
    public AuditActorResolver auditActorResolver(final TokenClaimsReader tokenClaimsReader) {
        return new AuditActorResolver(tokenClaimsReader);
    }

    @Bean
    public UseCaseTracing auditUseCaseTracing(final ObjectProvider<Tracer> tracer) {
        final var available = tracer.getIfAvailable();
        if (available == null) {
            return new StandaloneUseCaseTracing(new SecureRandom());
        }
        return new MicrometerUseCaseTracing(available);
    }

    @Bean
    public SourceIdentity auditSourceIdentity(final Environment environment, final AuditProperties properties,
                                              final ObjectProvider<BuildProperties> buildProperties) {
        final var build = buildProperties.getIfAvailable();
        final var version = build == null ? properties.sourceVersion() : build.getVersion();
        return new SourceIdentity(environment.getRequiredProperty("spring.application.name"), version);
    }

    @Bean
    public AuditTenantResolver auditTenantResolver(final ObjectProvider<TenantIdentifierService> tenants) {
        final var available = tenants.getIfAvailable();
        if (available == null) {
            return AuditTenantResolver.singleTenant();
        }
        return new AuditTenantResolver(available::resolveCurrentTenantIdentifier);
    }

    @Bean
    public AuditEventAssembler auditEventAssembler(final SourceIdentity auditSourceIdentity,
                                                   final AuditTenantResolver auditTenantResolver) {
        final var eventIds = new UuidV7Generator(Clock.systemUTC(), new SecureRandom());
        return new AuditEventAssembler(eventIds, auditSourceIdentity, auditTenantResolver);
    }

    @Bean
    public AuditPolicy auditPolicy(final AuditProperties properties) {
        return new AuditPolicy(properties.includeReadOnly());
    }

    @Bean
    @ConditionalOnProperty(prefix = "audit", name = "sink", havingValue = "log", matchIfMissing = true)
    public AuditEventSink auditEventSink(final AuditProperties properties, final Pseudonymizer auditPseudonymizer) {
        return new Slf4jAuditEventSink(AuditMappers.masking(properties, auditPseudonymizer));
    }

    @Bean
    public AuditTrailAspect auditTrailAspect(final AuditEventSink auditEventSink,
                                             final AuditActorResolver auditActorResolver,
                                             final UseCaseTracing auditUseCaseTracing,
                                             final AuditEventAssembler auditEventAssembler,
                                             final AuditPolicy auditPolicy) {
        return new AuditTrailAspect(auditEventSink, auditActorResolver, auditUseCaseTracing, auditEventAssembler,
                auditPolicy);
    }

    @Bean
    public MdcTaskDecorator auditMdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
