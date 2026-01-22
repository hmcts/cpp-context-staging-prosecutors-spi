package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SPIPoliceCaseEjectedProcessorTest {

    private static String PROSECUTOR_CASE_ID = randomUUID().toString();

    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED = "stagingprosecutorsspi.command.handler.spi-prosecutioncase-ejected";


    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;


    @InjectMocks
    private SPIPoliceCaseEjectedProcessor SPIPoliceCaseEjectedProcessor;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenProsecutionCaseIdPresentInPayload_expectCommandRaisedForCaseEjected() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("hearingIds", JsonObjects.createArrayBuilder().add(randomUUID().toString()).build())
                .add("prosecutionCaseId", PROSECUTOR_CASE_ID)
                .add("removalReason", "legal")
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"),
                payload);
        SPIPoliceCaseEjectedProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender, times(1)).sendAsAdmin(envelopeArgumentCaptor.capture());
        final JsonEnvelope captorValue = envelopeArgumentCaptor.getValue();
        assertThat(captorValue.payloadAsJsonObject().getString("caseId"), is(PROSECUTOR_CASE_ID));
        assertThat(captorValue.metadata().name(), is(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_EJECTED));
    }

    @Test
    public void testHandleCaseOrApplicationEjected_whenProsecutionCaseIdNotPresentInPayload_expectNoCommandRaisedForCaseEjected() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add("hearingIds", JsonObjects.createArrayBuilder().add(randomUUID().toString()).build())
                .add("applicationId", PROSECUTOR_CASE_ID)
                .add("removalReason", "legal")
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.progression.events.case-or-application-ejected"),
                payload);
        SPIPoliceCaseEjectedProcessor.handleCaseOrApplicationEjected(envelope);

        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());

    }


}
