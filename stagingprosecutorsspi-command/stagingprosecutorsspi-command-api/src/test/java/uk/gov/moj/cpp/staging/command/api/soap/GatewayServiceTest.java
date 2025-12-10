package uk.gov.moj.cpp.staging.command.api.soap;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_DESTINATION_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_EXEC_MODE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_REQUEST_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_SOURCE_ID;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.endpoint.types.RetrieveRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.justice.services.adapter.rest.envelope.RestEnvelopeBuilderFactory;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorContext;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.MDIValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GatewayServiceTest {

    public static final String TEST_MESSAGE = "Test Message";
    private static final String RECEIVE_CJSE_MESSAGE_COMMAND_API = "hmcts.cjs.receive-spi-message";


    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebServiceContext webServiceContext;

    @Spy
    private RestEnvelopeBuilderFactory restEnvelopeBuilderFactory;

    @Mock
    private Map<String, List<String>> http_headers;

    @Mock
    private MDIValidator mdiValidator;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private RouteDataRequestType routeDataType;

    @Mock
    private JsonObject cjseMessageJsonObject;

    @Mock
    private InterceptorChainProcessor dispatcher;
    @Captor
    private ArgumentCaptor<InterceptorContext> interceptorContextArgumentCaptor;
    @Mock
    private SubmitRequest submitRequestMes;

    @InjectMocks
    private GatewayService gatewayService;

    private static String SOURCE_ID = "SOURCE_ID";
    private static String REQUEST_ID = "REQUEST_ID";
    private static List<String> DESTINATION_ID = new ArrayList<String>();

    @Test
    public void shouldDispatchWithCorrectActionAndUserId() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);
        final UUID mockId = randomUUID();
        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(Arrays.asList(mockId.toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(Optional.empty());

        gatewayService.submit(submitRequestMes);

        verify(dispatcher).process(interceptorContextArgumentCaptor.capture());
        final JsonEnvelope jsonEnvelope = interceptorContextArgumentCaptor.getValue().inputEnvelope();

        assertThat(jsonEnvelope.metadata().name(), is(RECEIVE_CJSE_MESSAGE_COMMAND_API));
        assertThat(jsonEnvelope.metadata().userId().get(), is(mockId.toString()));
    }

    @Test
    public void shouldReturnSuccessSubmitResponseWithCorrectRequestIdAfterSuccessfulDispatch() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);
        final UUID mockRequestId = randomUUID();
        when(submitRequestMes.getRequestID()).thenReturn(mockRequestId.toString());
        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(Arrays.asList(randomUUID().toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(Optional.empty());

        final SubmitResponse submitResponse = gatewayService.submit(submitRequestMes);

        assertThat(submitResponse.getResponseCode(), is(1));
        assertThat(submitResponse.getResponseText(), is("Success"));
        assertThat(submitResponse.getRequestID(), is(mockRequestId.toString()));
    }

    @Test
    public void shouldReturnRetrieveNotAllowed() {
        final RetrieveRequest retrieveRequest = new RetrieveRequest();
        final UUID mockRequestId = randomUUID();
        retrieveRequest.setRequestID(mockRequestId.toString());

        final SubmitResponse submitResponse = gatewayService.retrieve(retrieveRequest);

        assertThat(submitResponse.getResponseCode(), is(309));
        assertThat(submitResponse.getResponseText(), is("RetrieveNotAvailable"));
        assertThat(submitResponse.getRequestID(), is(mockRequestId.toString()));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenSourceIdInvalid() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);
        final UUID mockRequestId = randomUUID();
        when(submitRequestMes.getRequestID()).thenReturn(mockRequestId.toString());
        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(Arrays.asList(randomUUID().toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(of(INVALID_SOURCE_ID));

        final SubmitResponse submitResponse = gatewayService.submit(submitRequestMes);

        assertThat(submitResponse.getResponseCode(), is(INVALID_SOURCE_ID.getCode()));
        assertThat(submitResponse.getResponseText(), is(INVALID_SOURCE_ID.getText()));
        assertThat(submitResponse.getRequestID(), is(mockRequestId.toString()));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenDestinationIdInvalid() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);
        final UUID mockRequestId = randomUUID();
        when(submitRequestMes.getRequestID()).thenReturn(mockRequestId.toString());
        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(Arrays.asList(randomUUID().toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(of(INVALID_DESTINATION_ID));

        final SubmitResponse submitResponse = gatewayService.submit(submitRequestMes);

        assertThat(submitResponse.getResponseCode(), is(INVALID_DESTINATION_ID.getCode()));
        assertThat(submitResponse.getResponseText(), is(INVALID_DESTINATION_ID.getText()));
        assertThat(submitResponse.getRequestID(), is(mockRequestId.toString()));
    }

    @Test
    public void shouldReturnAppropriateErrorWhenRequestIdInvalid() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);

        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(Arrays.asList(randomUUID().toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(of(INVALID_REQUEST_ID));

        final SubmitResponse submitResponse = gatewayService.submit(submitRequestMes);

        assertThat(submitResponse.getResponseCode(), is(INVALID_REQUEST_ID.getCode()));
        assertThat(submitResponse.getResponseText(), is(INVALID_REQUEST_ID.getText()));

    }

    @Test
    public void shouldReturnAppropriateErrorWhenExecModeInvalid() {
        when(objectToJsonObjectConverter.convert(any(CjseMessage.class))).thenReturn(cjseMessageJsonObject);
        when(webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS)).thenReturn(http_headers);
        when(http_headers.get(HeaderConstants.USER_ID)).thenReturn(asList(randomUUID().toString()));
        when(mdiValidator.validate(submitRequestMes)).thenReturn(of(INVALID_EXEC_MODE));

        final SubmitResponse submitResponse = gatewayService.submit(submitRequestMes);

        assertThat(submitResponse.getResponseCode(), is(INVALID_EXEC_MODE.getCode()));
        assertThat(submitResponse.getResponseText(), is(INVALID_EXEC_MODE.getText()));
    }

}
