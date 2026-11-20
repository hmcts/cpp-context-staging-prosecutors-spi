package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CourtCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CourtDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CourtHearingSession;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CourtOffence;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CourtSession;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.ResultConverter;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.service.ProsecutionCaseFileService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import jakarta.json.JsonObject;
import jakarta.xml.bind.JAXBException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.createPublicPoliceResultGeneratedFromJson;

/**
 * J17/J25 parity characterisation (J17 golden master), SPI-M08.
 *
 * Pins the exact XML that ResultProcessor sends in send-spi-result for a fixed police-result-generated payload:
 * the real ResultConverter chain plus the JAXB marshaller (JAXB_FRAGMENT). The J25 upgrade moves to Jakarta XML
 * Binding 4 with regenerated classes, so namespace prefixes, element order, date formats or nil handling could
 * change without any assertion noticing; until now the payload was only checked for notNullValue.
 *
 * The converter reads the JVM default time zone, so the test fixes it to UTC.
 */
@ExtendWith(MockitoExtension.class)
class ResultProcessorXmlParityTest {

    private static final String POLICE_RESULT_GENERATED_JSON = "json/public-police-result-generated.json";
    private static final String EXPECTED_SPI_RESULT_PAYLOAD_XML = "parity/expected-spi-result-payload.xml";

    @Mock
    private Sender sender;

    @Mock
    private Requester requester;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Captor
    private ArgumentCaptor<Envelope<SendSpiResult>> sendSpiResultCaptor;

    private TimeZone originalDefaultTimeZone;

    private final ResultProcessor resultProcessor = new ResultProcessor();

    @BeforeEach
    void setUp() {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        setField(resultProcessor, "sender", sender);
        setField(resultProcessor, "requester", requester);
        setField(resultProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        setField(resultProcessor, "prosecutionCaseFileService", prosecutionCaseFileService);
        setField(resultProcessor, "resultConverter", realResultConverter());
    }

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    @Test
    void spiM08_shouldSendTheJ17ResultXmlForAFixedPoliceResult() throws JAXBException, IOException {
        final PublicPoliceResultGenerated policeResult = createPublicPoliceResultGeneratedFromJson(POLICE_RESULT_GENERATED_JSON);
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.results.police-result-generated"), toJson(policeResult));
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(policeResult);
        when(prosecutionCaseFileService.extractOffenceLocation(envelope, requester, policeResult.getCaseId()))
                .thenReturn(Map.of("b9abaf2b-a948-4a66-8022-c8aa12317c81", "London"));
        when(requester.requestAsAdmin(any(), any())).thenReturn(cppMessages());
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        resultProcessor.onSpiCaseGenerated(envelope);

        verify(sender).send(sendSpiResultCaptor.capture());
        assertThat(sendSpiResultCaptor.getValue().payload().getSpiResult().getPayload(), is(readResource(EXPECTED_SPI_RESULT_PAYLOAD_XML)));
    }

    private ResultConverter realResultConverter() {
        final CourtOffence courtOffence = new CourtOffence();
        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);
        final CourtDefendant courtDefendant = new CourtDefendant();
        setField(courtDefendant, "courtOffence", courtOffence);
        final CourtCase courtCase = new CourtCase();
        setField(courtCase, "courtDefendant", courtDefendant);
        final CourtSession courtSession = new CourtSession();
        setField(courtSession, "courtCase", courtCase);
        setField(courtSession, "courtHearingSession", new CourtHearingSession());
        final ResultConverter resultConverter = new ResultConverter();
        setField(resultConverter, "courtSession", courtSession);
        return resultConverter;
    }

    private static JsonObject toJson(final PublicPoliceResultGenerated policeResult) {
        final ObjectToJsonObjectConverter converter = new ObjectToJsonObjectConverter();
        setField(converter, "mapper", new ObjectMapperProducer().objectMapper());
        return converter.convert(policeResult);
    }

    private static Envelope<Object> cppMessages() {
        final JsonObject cppMessage = createObjectBuilder()
                .add("policeSystemId", "3232")
                .add("organizationUnitID", "232")
                .add("dataController", "34343")
                .build();
        return Envelope.envelopeFrom(metadataWithRandomUUID("stagingprosecutorsspi.query.cpp-message"),
                createObjectBuilder().add("cppMessages", createArrayBuilder().add(cppMessage)).build());
    }

    private static String readResource(final String path) throws IOException {
        try (InputStream inputStream = ResultProcessorXmlParityTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(inputStream.readAllBytes(), UTF_8);
        }
    }
}
