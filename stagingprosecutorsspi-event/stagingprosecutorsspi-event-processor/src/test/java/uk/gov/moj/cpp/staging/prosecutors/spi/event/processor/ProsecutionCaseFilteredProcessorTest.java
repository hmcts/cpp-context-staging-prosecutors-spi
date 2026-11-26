package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ProsecutionCaseFiltered;

import java.util.UUID;

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
public class ProsecutionCaseFilteredProcessorTest {

    private static final String STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_FILTERED = "stagingprosecutorsspi.event.spi-police-case-filtered";
    private static final String PUBLIC_CASE_FILTERED = "public.stagingprosecutorsspi.event.prosecution-case-filtered";

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @InjectMocks
    private ProsecutionCaseFilteredProcessor processor;

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void processProsecutionCaseFiltered() {
        final ProsecutionCaseFiltered prosecutionCaseFiltered = ProsecutionCaseFiltered.prosecutionCaseFiltered()
                .withCaseId(UUID.randomUUID())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(prosecutionCaseFiltered);

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID(STAGINGPROSECUTORSSPI_COMMAND_HANDLER_SPI_PROSECUTIONCASE_FILTERED),
                payload);

        processor.handleProsecutionCaseFiltered(requestMessage);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_CASE_FILTERED));
        JsonObject actualPayload = publicEvent.payload();
        assertThat(actualPayload.getString("caseId"), equalTo(prosecutionCaseFiltered.getCaseId().toString()));

    }
}
