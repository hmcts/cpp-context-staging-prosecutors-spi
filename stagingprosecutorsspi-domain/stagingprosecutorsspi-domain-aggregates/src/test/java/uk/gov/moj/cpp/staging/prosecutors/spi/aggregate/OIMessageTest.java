package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.ErrorsReportedWithCPPResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OperationalDetailsPreparedForResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OiValidationErrorsFound;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.OIValidator;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OIMessageTest {

    private static final String CORRELATION_ID = randomUUID().toString();
    private static final String SYSTEM_ID = randomUUID().toString();
    private static final String POLICE_SYSTEM_ID = randomUUID().toString();
    private static final String DATA_STREAM_CONTENT = "Data stream content";
    private final UUID streamId = randomUUID();
    @Mock
    SpiResult spiResult;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OIDetails oiDetails;
    @Mock
    private OIValidator oiValidator;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RouteDataRequestType routeDataRequestType;
    private List<?> eventList;

    @BeforeEach
    public void setUp() {
        when(oiDetails.getRouteDataRequestType()).thenReturn(routeDataRequestType);
        when(routeDataRequestType.getRequestFromSystem().getCorrelationID()).thenReturn(CORRELATION_ID);
        when(routeDataRequestType.getRequestFromSystem().getSystemID().getValue()).thenReturn(SYSTEM_ID);
    }

    @Test
    public void shouldCreateCorrectEventWhenReceivingOIMessageWithErrors() {
        final OIMessage oiMessage = new OIMessage();
        final List<Optional<ValidationError>> validationError = Arrays.asList(Optional.of(ValidationError.valueOf("INVALID_SYSTEM_ID_CODE")));

        when(oiValidator.validate(any(RouteDataRequestType.class), any(OIInputVO.class))).thenReturn(validationError);

        final Stream<Object> eventStream = oiMessage.oiRequestReceived(oiDetails, streamId, oiValidator);
        eventList = eventStream.collect(Collectors.toList());

        assertEquals(2, eventList.size());
        assertEquals(OiValidationErrorsFound.class, eventList.get(0).getClass());
        assertEquals(OperationalDetailsPreparedForResponse.class, eventList.get(1).getClass());
    }

    @Test
    public void shouldCreateCorrectEventWhenReceivingOIMessage() {
        when(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue()).thenReturn(POLICE_SYSTEM_ID);
        setupOIMessage();

        assertEquals(1, eventList.size());
        assertEquals(OiRequestMessageReceived.class, eventList.get(0).getClass());
    }

    @Test
    public void shouldCreateEventWithCorrectCorrelationIdAndMSystemIdWhenReceivingOIMessage() {
        when(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue()).thenReturn(POLICE_SYSTEM_ID);
        setupOIMessage();

        final OiRequestMessageReceived oiRequestMessageReceived = (OiRequestMessageReceived) eventList.get(0);

        assertThat(oiRequestMessageReceived.getCorrelationId(), is(CORRELATION_ID));
        assertThat(oiRequestMessageReceived.getSystemId(), is(SYSTEM_ID));
        assertThat(oiRequestMessageReceived.getPoliceSystemId(), is(POLICE_SYSTEM_ID));
    }

    @Test
    public void oiResponseReceivedErrorsReportedWithCPPResponse() {
        final OIMessage oiMessage1 = new OIMessage();
        final List<Optional<ValidationError>> validationError = Arrays.asList(Optional.of(ValidationError.valueOf("INVALID_SYSTEM_ID_CODE")));

        when(oiValidator.validate(any(RouteDataRequestType.class), any(OIInputVO.class))).thenReturn(validationError);

        final Stream<Object> eventStream1 = oiMessage1.oiRequestReceived(oiDetails, streamId, oiValidator);
        eventList = eventStream1.collect(Collectors.toList());
        final OIMessage oiMessage = oiMessage1;
        final Stream<Object> eventStream = oiMessage.oiResponseReceived(oiDetails);
        List<Object> list = eventStream.filter(ErrorsReportedWithCPPResponse.class::isInstance).collect((Collectors.toList()));
        assertEquals(2, eventList.size());
        assertThat(list.get(0), instanceOf(ErrorsReportedWithCPPResponse.class));
    }

    @Test
    public void shouldRaiseErrorsReportedEventForCjseErrorResponse() {
        when(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue()).thenReturn(POLICE_SYSTEM_ID);
        final OIMessage oiMessage = setupOIMessage();
        oiMessage.prepareSPIResult(randomUUID(), spiResult, randomUUID().toString(), randomUUID());
        final Stream<Object> eventStream = oiMessage.oiResponseReceived(oiDetails);
        List<Object> list = eventStream.filter(ErrorsReportedWithCPPResponse.class::isInstance).collect((Collectors.toList()));
        assertThat("Unexpected number of events", 1, is(eventList.size()));
        assertThat(list.get(0), instanceOf(ErrorsReportedWithCPPResponse.class));
    }

    @Test
    public void prepareOIResponseForXSDFailuresTest() {
        when(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue()).thenReturn(POLICE_SYSTEM_ID);
        final OIMessage oiMessage = setupOIMessage();
        final Stream<Object> eventStream = oiMessage.prepareOIResponseForXSDFailures("This is a Error Message for XSDFailures");
        List<Object> list = eventStream.filter(OperationalDetailsPreparedForResponse.class::isInstance).collect((Collectors.toList()));
        assertEquals(1, list.size());
        assertThat(list.get(0), instanceOf(OperationalDetailsPreparedForResponse.class));

    }

    @Test
    public void prepareSPIResultTest() {
        when(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue()).thenReturn(POLICE_SYSTEM_ID);
        final OIMessage oiMessage = setupOIMessage();
        final Stream<Object> eventStream = oiMessage.prepareSPIResult(randomUUID(), spiResult, randomUUID().toString(), randomUUID());
        List<Object> list = eventStream.filter(SpiResultPreparedForSending.class::isInstance).collect((Collectors.toList()));
        assertEquals(1, list.size());
        assertThat(list.get(0), instanceOf(SpiResultPreparedForSending.class));

    }

    private OIMessage setupOIMessage() {
        final OIMessage oiMessage = new OIMessage();
        when(routeDataRequestType.getDataStream().getDataStreamContent()).thenReturn(DATA_STREAM_CONTENT);
        final Stream<Object> eventStream = oiMessage.oiRequestReceived(oiDetails, streamId, oiValidator);
        eventList = eventStream.collect(Collectors.toList());
        return oiMessage;
    }

}
