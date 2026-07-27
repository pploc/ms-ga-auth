package com.gymapi.auth.domain.exception;

/**
 * Raised when a domain event could not be handed to the broker.
 *
 * <p>Publishing happens inside the same transaction as the state change, so throwing here rolls the
 * change back. That is the at-least-once trade the service makes deliberately: a consumer may see
 * an event twice, but an RBAC change never lands silently without the rest of the platform hearing
 * about it.
 *
 * <p>Surfaces as 503, because the caller retrying the identical request — ideally with the same
 * {@code Idempotency-Key} — is the correct response.
 */
public class EventPublishFailedException extends DomainException {

  public EventPublishFailedException(String message, Throwable cause) {
    super(ErrorCode.EVENT_PUBLISH_FAILED, message, cause);
  }

  public static EventPublishFailedException of(String eventType, Throwable cause) {
    return new EventPublishFailedException(
        "Could not publish event " + eventType + "; the change was rolled back", cause);
  }
}
