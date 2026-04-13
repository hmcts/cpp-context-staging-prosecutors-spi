package uk.gov.moj.cpp.staging.prosecutorapi.query.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CPPMessageQueryApiTest {

    @InjectMocks
    private CPPMessageQueryApi api;
    @Mock
    private Requester requester;
    @Mock
    private JsonEnvelope expectedEnvelope;
    @Captor
    private ArgumentCaptor<JsonEnvelope> jsonEnvelopeArgumentCaptor;

    @Test
    public void queriesForSubmission() {
        final String ptiUrn = UUID.randomUUID().toString();
        JsonEnvelope envelope = createEnvelope("stagingprosecutorsspi.query.cpp-message",
                createObjectBuilder()
                        .add("ptiUrn", ptiUrn)
                        .build()
        );

        api.queryCppMessage(envelope);
        verify(requester).request(jsonEnvelopeArgumentCaptor.capture());

        final JsonEnvelope jsonEnvelope = jsonEnvelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("stagingprosecutorsspi.query.cpp-message-view"));
        assertThat(jsonEnvelope.payloadAsJsonObject().getString("ptiUrn"), is(ptiUrn));
    }

}