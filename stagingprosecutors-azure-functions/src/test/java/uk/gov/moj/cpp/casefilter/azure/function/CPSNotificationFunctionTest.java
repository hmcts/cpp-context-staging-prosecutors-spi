package uk.gov.moj.cpp.casefilter.azure.function;

import static java.util.Optional.of;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtils.getPayload;

import uk.gov.moj.cpp.casefilter.azure.exception.MissingFieldException;
import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;
import uk.gov.moj.cpp.casefilter.azure.service.CpsPayloadTransformService;
import uk.gov.moj.cpp.casefilter.azure.service.QueryMaterialAndSendNotificationService;

import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CPSNotificationFunctionTest {

    @Mock
    private ExecutionContext context;
    @Mock
    private Logger logger;
    @Mock
    private HttpRequestMessage<Optional<String>> requestMessage;
    @Mock
    private QueryMaterialAndSendNotificationService queryMaterialAndSendNotificationService;
    @Mock
    private CpsPayloadTransformService cpsPayloadTransformService;
    @Mock
    private HttpResponseMessage httpResponseMessage;
    @Mock
    private HttpResponseMessage.Builder builder;

    private final CPSNotificationFunction target = new CPSNotificationFunction();

    @BeforeEach
    public void setUp() {
        setField(target, "queryMaterialAndSendNotificationService", queryMaterialAndSendNotificationService);
        setField(target, "cpsPayloadTransformService", cpsPayloadTransformService);
    }

    @Test
    public void shouldCpsNotificationSuccessForValidPayload() {
        when(requestMessage.createResponseBuilder(any(HttpStatus.class))).thenReturn(builder);
        when(builder.body(any())).thenReturn(builder);
        when(builder.build()).thenReturn(httpResponseMessage);
        when(context.getLogger()).thenReturn(logger);
        when(requestMessage.getBody()).thenReturn(of(getPayload("NotificationInputPayload.json")));
        when(cpsPayloadTransformService.transform(any(JsonObject.class))).thenReturn(createObjectBuilder().build());
        final String requestPayload = getPayload("NotificationInputPayload.json");
        final ResponseDto responseDto = getResponseDto(false);

        when(httpResponseMessage.getStatusCode()).thenReturn(SC_OK);
        when(httpResponseMessage.getBody()).thenReturn(responseDto.getMessageBody());
        when(requestMessage.getBody()).thenReturn(of(requestPayload));
        when(queryMaterialAndSendNotificationService.getMaterialById(anyString(), anyString(), any(JsonObject.class), any(Logger.class))).thenReturn(responseDto);

        final HttpResponseMessage responseMessage = target.cpsNotification(requestMessage, context);

        assertThat(responseMessage.getStatusCode(), is(SC_OK));
        assertThat(responseMessage.getBody(), is(responseDto.getMessageBody()));

        verifyInvocations();
    }

    @Test
    public void shouldCpsNotificationFailForInvalidPayload() {
        when(context.getLogger()).thenReturn(logger);
        when(requestMessage.getBody()).thenReturn(of(getPayload("NotificationInputPayload.json")));
        final String requestPayload = "{}";
        when(requestMessage.getBody()).thenReturn(of(requestPayload));

        assertThrows(MissingFieldException.class, () -> target.cpsNotification(requestMessage, context));
    }

    private void verifyInvocations() {
        verify(builder).build();
        verify(requestMessage).getBody();
        verify(queryMaterialAndSendNotificationService).getMaterialById(anyString(), anyString(), any(JsonObject.class), any(Logger.class));
        verify(requestMessage).createResponseBuilder(any(HttpStatus.class));
        verify(builder).body(any());
        verify(context).getLogger();
        verify(cpsPayloadTransformService).transform(any(JsonObject.class));
    }

    private ResponseDto getResponseDto(final boolean isFailed) {
        if (isFailed) {
            return new ResponseDto(SC_BAD_REQUEST, "Failed");
        }
        return new ResponseDto(SC_OK, "Successful");
    }
}