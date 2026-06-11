package gn.lacaeducation.webhooks.adapters.out.persistence;
import gn.lacaeducation.webhooks.domain.*;
import org.mapstruct.Mapper;
@Mapper(componentModel = "spring")
interface WebhooksMapper {
    WebhookEndpoint toDomain(WebhookEndpointJpaEntity e);
    WebhookEndpointJpaEntity toEntity(WebhookEndpoint d);
    WebhookEvent toDomain(WebhookEventJpaEntity e);
    WebhookEventJpaEntity toEntity(WebhookEvent d);
    PaymentWebhook toDomain(PaymentWebhookJpaEntity e);
    PaymentWebhookJpaEntity toEntity(PaymentWebhook d);
}
