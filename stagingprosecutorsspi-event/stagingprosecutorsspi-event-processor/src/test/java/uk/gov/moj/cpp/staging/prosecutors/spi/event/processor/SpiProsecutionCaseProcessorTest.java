package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiProsecutionCaseProcessor.PREPARE_ASYNC_ERROR_RESPONSE_COMMAND;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiProsecutionCaseProcessor.PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.processor.SpiProsecutionCaseProcessor.PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelopeProvider;
import uk.gov.justice.services.messaging.spi.EnvelopeProvider;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.PrepareAsyncErrorResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.SpiToPCFConverter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseUpdateReceivedWithDifferentInitiationCode;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ProsecutionCaseReceivedWithMultipleDefendants;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiNewDefendantsReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;
import uk.gov.moj.cps.prosecutioncasefile.command.api.InitiateProsecution;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicProsecutionCaseUnsupported;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiProsecutionCaseProcessorTest {

    private static final UUID FIELD_OID = UUID.randomUUID();
    private static final String POLICE_SYSTEM_ID = new StringGenerator().next();
    private static final String PTI_URN = new StringGenerator().next();

    @Mock
    private Envelope<SpiProsecutionCaseReceived> spiProsecutionCaseReceivedEnvelope;
    @Mock
    private Envelope<SpiNewDefendantsReceived> spiNewDefendantsReceivedEnvelope;
    @Mock
    private Sender sender;
    @Mock
    private SpiProsecutionCaseReceived spiProsecutionCaseReceived;

    @Mock
    private PoliceCase policeCase;

    @Mock
    private CaseDetails caseDetails;
    @Mock
    private PoliceDefendant policeDefendant;

    @Spy
    final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private InitiateProsecution initiateProsecution;

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final SpiToPCFConverter spiToPCFConverter = new SpiToPCFConverter();

    @Mock
    private Envelope<ProsecutionCaseReceivedWithMultipleDefendants> sjpCaseReceivedWithMultipleDefendantsEnvelope;

    @Mock
    private Envelope<CaseUpdateReceivedWithDifferentInitiationCode> caseUpdateReceivedWithDifferentInitiationCodeEnvelope;

    @Mock
    private Envelope<PublicProsecutionCaseUnsupported> prosecutionCaseUnsupportedCodeEnvelope;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PrepareAsyncErrorResponse>> envelopePrepareErrorSyncResponseCaptor;

    @Mock
    private JsonObject jsonObject;

    @Spy
    private final Clock clock = new StoppedClock(now(UTC));

    final EnvelopeProvider envelopeProvider = new DefaultEnvelopeProvider();

    @InjectMocks
    private SpiProsecutionCaseProcessor spiProsecutionCaseProcessor;

    @Test
    public void shouldForwardSingleDefendantSJPCaseToATCM() throws IOException {
        final URL eventUrl = this.getClass().getClassLoader().getResource("stagingprosecutorsspi.event.prosecution-case-received-for-sjp.json");
        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = objectMapper.readValue(eventUrl, SpiProsecutionCaseReceived.class);
        final Envelope<SpiProsecutionCaseReceived> spiProsecutionCaseReceivedEnvelope = envelopeProvider.envelopeFrom(getMetaData(), spiProsecutionCaseReceived);

        spiProsecutionCaseProcessor.onSpiProsecutionCaseReceived(spiProsecutionCaseReceivedEnvelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> captorValue = envelopeArgumentCaptor.getValue();
        final JsonObject actualPayload = captorValue.payload();
        final Metadata actualMetadata = captorValue.metadata();
        assertThat(actualPayload, is(notNullValue()));
        assertThat(actualMetadata.name(), is(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION));

        assertThat(actualMetadata.createdAt().isPresent(), is(true));
        assertThat(actualPayload.toString(), isJson(
                withJsonPath("$.caseDetails.dateReceived", equalTo(LocalDates.to(actualMetadata.createdAt().get().toLocalDate())))
        ));
    }

    @Test
    public void shouldForwardSingleDefendantNonSJPCaseToCC() throws IOException {
        final URL eventUrl = this.getClass().getClassLoader().getResource("stagingprosecutorsspi.event.prosecution-case-received-for-cc.json");
        final SpiProsecutionCaseReceived spiProsecutionCaseReceived = objectMapper.readValue(eventUrl, SpiProsecutionCaseReceived.class);
        final Envelope<SpiProsecutionCaseReceived> spiProsecutionCaseReceivedEnvelope = envelopeProvider.envelopeFrom(getMetaData(), spiProsecutionCaseReceived);

        spiProsecutionCaseProcessor.onSpiProsecutionCaseReceived(spiProsecutionCaseReceivedEnvelope);

        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        final Envelope<JsonObject> captorValue = envelopeArgumentCaptor.getValue();
        final JsonObject actualPayload = captorValue.payload();
        final Metadata actualMetadata = captorValue.metadata();

        assertThat(actualPayload, is(notNullValue()));
        assertThat(actualMetadata.name(), is(PROSECUTIONCASEFILE_COMMAND_INITIATE_CC_PROSECUTION));
        assertThat(actualMetadata.createdAt().isPresent(), is(true));
        assertThat(actualPayload.toString(), isJson(
                withJsonPath("$.caseDetails.dateReceived", equalTo(LocalDates.to(actualMetadata.createdAt().get().toLocalDate())))
        ));
    }

    @Test
    public void shouldSendCorrectPayloadOnCaseUnsupportedMessageReceived() {
        final Metadata metadata = getMetaData();
        final PublicProsecutionCaseUnsupported prosecutionCaseUnsupported = buildProsecutionCaseUnsupported(Channel.SPI);

        when(prosecutionCaseUnsupportedCodeEnvelope.metadata()).thenReturn(metadata);
        when(prosecutionCaseUnsupportedCodeEnvelope.payload()).thenReturn(prosecutionCaseUnsupported);
        spiProsecutionCaseProcessor.onCaseUnsupportedMessageReceived(prosecutionCaseUnsupportedCodeEnvelope);

        verify(sender).send(envelopePrepareErrorSyncResponseCaptor.capture());
        final Envelope<PrepareAsyncErrorResponse> captorValue = envelopePrepareErrorSyncResponseCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.payload().getErrorMessage(), is(prosecutionCaseUnsupported.getErrorMessage()));
        assertThat(captorValue.payload().getOiId(), is(prosecutionCaseUnsupported.getExternalId()));
        assertThat(captorValue.payload().getPoliceSystemId(), is(prosecutionCaseUnsupported.getPoliceSystemId()));
        assertThat(captorValue.payload().getPtiUrn(), is(prosecutionCaseUnsupported.getUrn()));
        assertThat(captorValue.metadata().name(), is(PREPARE_ASYNC_ERROR_RESPONSE_COMMAND));
    }

    @Test
    public void shouldNotSendOnCaseUnsupportedMessageReceivedWhenChannelIsNotSpi() {
        final Metadata metadata = getMetaData();
        final PublicProsecutionCaseUnsupported prosecutionCaseUnsupported = buildProsecutionCaseUnsupported(Channel.CPPI);

        when(prosecutionCaseUnsupportedCodeEnvelope.payload()).thenReturn(prosecutionCaseUnsupported);
        spiProsecutionCaseProcessor.onCaseUnsupportedMessageReceived(prosecutionCaseUnsupportedCodeEnvelope);

        verifyNoInteractions(sender);
    }

    private PublicProsecutionCaseUnsupported buildProsecutionCaseUnsupported(Channel channel) {
        final PublicProsecutionCaseUnsupported prosecutionCaseUnsupported = PublicProsecutionCaseUnsupported.publicProsecutionCaseUnsupported()
                .withChannel(channel)
                .withExternalId(UUID.randomUUID())
                .withPoliceSystemId(RandomStringUtils.random(10))
                .withUrn(RandomStringUtils.random(5))
                .withErrorMessage(RandomStringUtils.random(50))
                .build();
        return prosecutionCaseUnsupported;
    }

    private Metadata getMetaData() {
        return metadataBuilder()
                .createdAt(now())
                .withCausation(randomUUID())
                .withName(PROSECUTIONCASEFILE_COMMAND_INITIATE_SJP_PROSECUTION)
                .withId(randomUUID())
                .build();
    }
}
