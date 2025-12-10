package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiOutMessageQueryApiTest {

    @InjectMocks
    private SpiOutMessageQueryApi api;
    @Mock
    private Requester requester;
    @Mock
    private JsonEnvelope expectedEnvelope;
    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Test
    public void queriesForSpiOutMessages() {
        final String caseUrn = randomAlphanumeric(8);
        final String defendantProsecutorReference = randomAlphanumeric(15);
        JsonEnvelope envelope = createEnvelope("stagingprosecutorsspi.query.spi-out-message",
                createObjectBuilder()
                        .add("caseUrn", caseUrn)
                        .add("defendantProsecutorReference", defendantProsecutorReference)
                        .build()
        );

        api.querySpiMessage(envelope);
        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("stagingprosecutorsspi.query.spi-out-message-view"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("caseUrn"), is(caseUrn));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("defendantProsecutorReference"), is(defendantProsecutorReference));
    }

}