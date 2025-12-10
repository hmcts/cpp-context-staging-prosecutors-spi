package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.createPublicPoliceResultGenerated;

import uk.gov.dca.xmlschemas.libra.StdProsPoliceResultedCaseStructure;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonMetadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.MdiIdWithMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.command.handler.SendSpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.ResultConverter;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultProcessorTest {

    private static final String SEND_SPI_RESULT_COMMAND = "stagingprosecutorsspi.command.handler.send-spi-result";
    private static final String SPI_RESULT_PREPARED_FOR_SENDING = "stagingprosecutorsspi.command.handler.prepare-cpp-message-for-sending";
    private static final String NON_POLICE_SYSTEM_ID = "00001NPPforB7";

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private Requester requester;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private ResultProcessor resultProcessor;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<Envelope<SendSpiResult>> sendSpiResultArgumentCaptor;

    @Mock
    private Envelope<SpiResultPreparedForSending> spiResultPreparedForSendingEnvelope;

    @Mock
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private ResultConverter resultConverter;

    @BeforeEach
    public void setUp() {
        initMocks(this);
        objectToJsonObjectConverter = new ObjectToJsonObjectConverter();
        setField(objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldRaiseAPublicEventPoliceResultGenerated() throws JAXBException {
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "policeSystemId", "3232",
                "organizationUnitID", "232",
                "dataController", "34343")));
        final PublicPoliceResultGenerated publicPoliceResultGenerated = createPublicPoliceResultGenerated();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.results.police-result-generated"),
                objectToJsonObjectConverter.convert(publicPoliceResultGenerated));
        final Map<String, Object> context = new HashMap<>();
        when(resultConverter.convert(eq(publicPoliceResultGenerated), eq(context))).thenReturn(new StdProsPoliceResultedCaseStructure());
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(publicPoliceResultGenerated);
        resultProcessor.onSpiCaseGenerated(envelope);

        verify(prosecutionCaseFileService).extractOffenceLocation(envelope, requester, publicPoliceResultGenerated.getCaseId());
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(SEND_SPI_RESULT_COMMAND));
    }

    @Test
    public void shouldRaiseAPublicEventPoliceResultGeneratedForNullDataControllerAndOrgId() throws JAXBException {
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "policeSystemId", "3232",
                "organizationUnitID", "232",
                "dataController", "34343")));
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of("policeSystemId", "3232")));


        final PublicPoliceResultGenerated publicPoliceResultGenerated = createPublicPoliceResultGenerated();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.results.police-result-generated"),
                objectToJsonObjectConverter.convert(publicPoliceResultGenerated));
        final Map<String, Object> context = new HashMap<>();
        when(resultConverter.convert(eq(publicPoliceResultGenerated), eq(context))).thenReturn(new StdProsPoliceResultedCaseStructure());
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(publicPoliceResultGenerated);
        resultProcessor.onSpiCaseGenerated(envelope);

        verify(prosecutionCaseFileService).extractOffenceLocation(envelope, requester, publicPoliceResultGenerated.getCaseId());
        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(notNullValue()));
        assertThat(captorValue.metadata().name(), is(SEND_SPI_RESULT_COMMAND));
    }

    @Test
    public void shouldGenerateSendSpiResultCommandWithNonPoliceSystemIdWhenCppMessageIsNull() throws JAXBException {
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "policeSystemId", "3232",
                "organizationUnitID", "232",
                "dataController", "34343")));
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(null));
        final SendSpiResult sendSpiResult = shouldGenerateSendSpiResultCommandForNonExistentPoliceSystemId();
        assertThat(sendSpiResult, notNullValue());
        assertThat(sendSpiResult.getSpiResult().getDestinationSystemId(), is(NON_POLICE_SYSTEM_ID));
        assertThat(sendSpiResult.getSpiResult().getDataController(), nullValue());
        assertThat(sendSpiResult.getSpiResult().getOrganizationalUnitID(), nullValue());
    }

    @Test
    public void shouldGenerateSendSpiResultCommandWithNonPoliceSystemIdWhenCppMessageHasNoCpsCases() throws JAXBException {
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "policeSystemId", "3232",
                "organizationUnitID", "232",
                "dataController", "34343")));
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(emptyMap()));
        final SendSpiResult sendSpiResult = shouldGenerateSendSpiResultCommandForNonExistentPoliceSystemId();
        assertThat(sendSpiResult, notNullValue());
        assertThat(sendSpiResult.getSpiResult().getDestinationSystemId(), is(NON_POLICE_SYSTEM_ID));
        assertThat(sendSpiResult.getSpiResult().getDataController(), nullValue());
        assertThat(sendSpiResult.getSpiResult().getOrganizationalUnitID(), nullValue());
    }

    @Test
    public void shouldGenerateSendSpiResultCommandWithNonPoliceSystemIdWhenCppMessageHasNoPoliceSystemId() throws JAXBException {
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "policeSystemId", "3232",
                "organizationUnitID", "232",
                "dataController", "34343")));
        when(requester.requestAsAdmin(any(), any())).thenReturn(getCppMessage(of(
                "organizationUnitID", "232",
                "dataController", "34343")));
        final SendSpiResult sendSpiResult = shouldGenerateSendSpiResultCommandForNonExistentPoliceSystemId();
        assertThat(sendSpiResult, notNullValue());
        assertThat(sendSpiResult.getSpiResult().getDestinationSystemId(), is(NON_POLICE_SYSTEM_ID));
    }

    public SendSpiResult shouldGenerateSendSpiResultCommandForNonExistentPoliceSystemId() throws JAXBException {
        final PublicPoliceResultGenerated publicPoliceResultGenerated = createPublicPoliceResultGenerated();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.results.police-result-generated"),
                objectToJsonObjectConverter.convert(publicPoliceResultGenerated));
        final Map<String, Object> context = new HashMap<>();
        when(resultConverter.convert(any(), eq(context))).thenReturn(new StdProsPoliceResultedCaseStructure());
        when(jsonObjectToObjectConverter.convert(any(), any())).thenReturn(publicPoliceResultGenerated);
        resultProcessor.onSpiCaseGenerated(envelope);

        verify(sender).send(sendSpiResultArgumentCaptor.capture());
        final Envelope<SendSpiResult> captorValue = sendSpiResultArgumentCaptor.getValue();
        assertThat(captorValue.metadata().name(), is(SEND_SPI_RESULT_COMMAND));

        return captorValue.payload();
    }

    @Test
    public void shouldRaiseAPublicEventSpiResultPreparedForSending() throws JAXBException {
        final Metadata metadata = getMetaData();
        SpiResultPreparedForSending spiResultPreparedForSending = createMockResultForSending();
        when(spiResultPreparedForSendingEnvelope.metadata()).thenReturn(metadata);
        when(spiResultPreparedForSendingEnvelope.payload()).thenReturn(spiResultPreparedForSending);

        resultProcessor.onSpiResultPreparedForSending(spiResultPreparedForSendingEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        final Envelope captorValue = envelopeArgumentCaptor.getValue();

        assertThat(captorValue.payload(), is(instanceOf(MdiIdWithMessage.class)));
        assertThat(captorValue.metadata().name(), is(SPI_RESULT_PREPARED_FOR_SENDING));

    }

    private SpiResultPreparedForSending createMockResultForSending() {
        return new SpiResultPreparedForSending(null, "payload", "ptiUrn", randomUUID(), randomUUID());

    }

    private Metadata getMetaData() {
        return DefaultJsonMetadata.metadataBuilder()
                .createdAt(ZonedDateTime.now())
                .withCausation(randomUUID())
                .withId(randomUUID())
                .withName("COMMAND")
                .withSource("Source")
                .build();
    }

    private Envelope<Object> getCppMessage(final Map<String, String> fieldsMap) {
        if (isNull(fieldsMap)) {
            return null;
        }

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        JsonObject jsonObject;
        JsonArray jsonArray;

        if (fieldsMap.isEmpty()) {
            jsonArray = createArrayBuilder().build();
        }
        else {
            fieldsMap.forEach((key, value) -> jsonObjectBuilder.add(key, value));
            jsonObject = jsonObjectBuilder.build();
            jsonArray = createArrayBuilder().add(jsonObject).build();
        }

        return Envelope.envelopeFrom(metadataBuilder().withId(randomUUID()).withName("public.results.police-result-generated"), jsonObjectBuilder
                .add("cppMessages",
                        jsonArray)
                .build());
    }
}