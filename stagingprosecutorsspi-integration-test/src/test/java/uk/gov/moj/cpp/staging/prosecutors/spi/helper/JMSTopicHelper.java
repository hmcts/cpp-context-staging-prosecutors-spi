package uk.gov.moj.cpp.staging.prosecutors.spi.helper;


import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class JMSTopicHelper {

    public static final String USER_ID = UUID.randomUUID().toString();

    public static void postMessageToTopicAndVerify(final String payload, final String publicEventName, final String expectedEventToBePublished) {
        final JmsMessageConsumerClient messageConsumerClient = newPrivateJmsMessageConsumerClientProvider("stagingprosecutorsspi")
                .withEventNames(expectedEventToBePublished).getMessageConsumerClient();

        final JmsMessageProducerClient jmsMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        jmsMessageProducerClient.sendMessage(publicEventName, buildJsonEnvelope(publicEventName, payload));

        assertThat(expectedEventToBePublished + " message not found in stagingprosecutorsspi.event topic", messageConsumerClient.retrieveMessage(10000L).isPresent(), is(true));
    }

    public static void postMessageToPublicTopic(final String payload, final String publicEventName) {
        final JmsMessageProducerClient jmsMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        jmsMessageProducerClient.sendMessage(publicEventName, buildJsonEnvelope(publicEventName, payload));
    }

    private static JsonEnvelope buildJsonEnvelope(String commandName, String payload) {
        final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
        final JsonObject jsonObjectPayload = stringToJsonObjectConverter.convert(payload);
        final Metadata metadata = getMetadataWithName(commandName, empty());

        return envelopeFrom(metadata, jsonObjectPayload);
    }

    private static Metadata getMetadataWithName(String commandName, Optional<UUID> streamId) {
        MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(commandName)
                .createdAt(ZonedDateTime.now())
                .withUserId(USER_ID)
                .withClientCorrelationId(UUID.randomUUID().toString());

        if (streamId.isPresent()) {
            metadataBuilder = metadataBuilder.withStreamId(streamId.get())
                    .withPosition(1);
        }

        return metadataBuilder.build();
    }
}
