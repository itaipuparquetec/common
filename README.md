# Apresentação

Este projeto é uma biblioteca common para várias aplicações Java WEB.

## Construção 🚧

- Sempre que fizer uma alteração, altere a versão no pom.xml, caso contrário o deploy dará erro.
    - Utilize versionamento semântico para isso (saiba mais em https://semver.org/lang/pt-BR/). o padrão semver para
      este versionamento Execute o arquivo ```api/src/main/resources/container/docker-compose.yml``` via
      ```docker compose up```;
- As credenciais do banco de dados, email, e authorization server estão aqui
  no [Sharepoint do SYSHUB](https://itaipuparquetec-my.sharepoint.com/:f:/r/personal/emanuel_fonseca_itaipuparquetec_org_br/Documents/F%C3%A1brica%20de%20Software?csf=1&web=1&e=Y1rMgY);
    - Você deverá inserí-las como variáveis de ambiente na sua estação local.
        - versão Maior(MAJOR): quando fizer mudanças incompatíveis na API,
        - versão Menor(MINOR): quando adicionar funcionalidades mantendo compatibilidade, e
        - versão de Correção(PATCH): quando corrigir falhas mantendo compatibilidade.Pode fazer isso através da PATH do
          seu sistema operacional, ou pela própria IDE (no caso do Jetbrains IDEA a opção é Environment Variables).
- Após o deploy, suba a versão no projeto cliente que utilizará a nova funcionalidade.

## Trilha de auditoria (starter) 🔎

Starter `br.org.itaipuparquetec.common.infrastructure.trail`, ativado por `audit.enabled=true`. Cada execução de
`UseCase`/`UnitUseCase`/`NullaryUseCase` gera um evento (envelope v1, contrato em
`src/main/resources/trail/trail-event-v1.schema.json`) e abre um *span* filho do rastreio W3C em vigor
(ADR-032). Decisões: ADR-030, ADR-031 e ADR-032.

### Propriedades

| Propriedade | Padrão | Descrição |
|---|---|---|
| `audit.enabled` | (ausente = desligado) | Liga a trilha. |
| `audit.sink` | `log` | `log` (log estruturado `AUDIT`, sem garantia de entrega) ou `outbox` (outbox transacional + Kafka). |
| `audit.include-read-only` | `false` | Audita também casos de uso em transação `readOnly`. Por padrão só escritas e leituras marcadas com `@AuditSensitiveRead`. |
| `audit.max-input-bytes` | `32768` | Acima disso o `input` é omitido (`inputOmitted=SIZE_LIMIT`). |
| `audit.source-version` | - | Versão do produtor quando não há `BuildProperties`. |
| `audit.topics.events` | `hubti.trail.events` | Tópico de eventos. |
| `audit.topics.dead-letter` | `hubti.trail.events.dlt` | Tópico DLT (usado pelo consumidor da trilha). |
| `audit.outbox.migrate-schema` | `true` | Cria `audit_outbox` na subida (ver abaixo). |
| `audit.outbox.deferred-writer-threads` | `2` | Threads que gravam eventos de falha/leitura após o fim da transação. |
| `audit.outbox.deferred-queue-capacity` | `10000` | Fila dessas gravações; excedente cai no log `AUDIT` + métrica. |
| `audit.relay.enabled` | `false` | Liga o relay (outbox para Kafka) nesta instância. |
| `audit.relay.interval` | `1s` | Pausa entre ciclos do relay. |
| `audit.relay.batch-size` | `100` | Linhas por tenant por ciclo. |
| `audit.relay.send-timeout` | `30s` | Espera máxima pelo ack do broker por lote. |
| `audit.relay.tenant-refresh` | `60s` | Atualização da lista de tenants. |
| `audit.relay.alert-attempts` | `10` | Tentativas de uma linha que disparam alerta (log `ERROR`). |
| `audit.cleartext.*`, `audit.pseudonymization-salt` | ver `AuditProperties` | Mascaramento (ADR-021). |

Para a entrega garantida: `audit.sink=outbox` e `audit.relay.enabled=true`, mais `spring.application.name`,
`spring.kafka.bootstrap-servers` (e a segurança do Kafka em produção) e `management.tracing.sampling.probability=1.0`
(os identificadores precisam existir em toda solicitação).

### Outbox e migração

- Tabela `audit_outbox` no schema do serviço, em cada banco de tenant. Script em
  `classpath:db/audit-migrations/1__create_audit_outbox.sql`.
- Serviços com `hubti.multitenancy.enabled=true`: o `PostgreSQLMigrationServiceImpl` aplica o script em cada tenant
  quando `audit.sink=outbox`. Serviços de banco único: o starter aplica na subida (`AuditOutboxSchemaInitializer`).
- O script usa histórico Flyway próprio (`audit_flyway_history`), sem colidir com as versões nem com a validação do
  `flyway_schema_history` do serviço. Não adicione a pasta `db/audit-migrations` às `locations` do Flyway do serviço.
- Sucesso de escrita: `INSERT` na mesma transação do negócio (se falhar, a operação de negócio falha). Falha,
  `readOnly` ou sem transação: gravação em transação própria depois do fim da transação de negócio (por uma thread
  de gravação, pois com `JpaTransactionManager` a conexão do negócio só é liberada depois dos callbacks de
  conclusão). Se a gravação falhar: log `AUDIT` + métrica `audit_outbox_write_failures`.
- Regra de arquitetura: o `@Transactional` deve estar no controller, nunca na implementação do caso de uso.

### Marcar leituras sensíveis

```java
@AuditSensitiveRead
public class GetComplaintUseCaseImpl implements UseCase<GetComplaintInput, ComplaintOutput> { ... }
```

### Rastreio (W3C Trace Context)

`micrometer-tracing-bridge-otel` e `spring-boot-starter-actuator` vêm transitivos. O contexto de rastreio vem do
Micrometer (servidor HTTP, listeners Kafka); sem `Tracer` o starter gera `traceId` (32 hex) e `spanId` (16 hex) por
conta própria. O contexto de MDC (`actor`, `sid`, `jti`, `traceId`, `spanId`) é restaurado ao fim de cada caso de uso,
inclusive aninhado.

### Métricas (Micrometer)

`audit_outbox_write_failures`, `audit_outbox_published`, `audit_outbox_publish_failures` (tag `service`) e os
*gauges* `audit_outbox_backlog` (limitado a 10001) e `audit_outbox_oldest_age_seconds` (tags `service`, `tenant`).

## Testes 🚀

- Se o coverage diminuir a biblioteca também quebrará na pipeline.

Enjoy 😎