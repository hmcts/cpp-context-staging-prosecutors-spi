package uk.gov.moj.cpp.staging.command.api.soap;

import static java.util.UUID.randomUUID;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.cjse.schemas.endpoint.types.RetrieveRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.justice.services.adapter.rest.envelope.RestEnvelopeBuilderFactory;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.MDIValidator;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.jws.HandlerChain;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.slf4j.Logger;

@Adapter(COMMAND_API)
@HandlerChain(file = "/META-INF/handlers.xml")
public class GatewayService {

    private static final Logger LOGGER = getLogger(GatewayService.class);

    @Inject
    RestEnvelopeBuilderFactory restEnvelopeBuilderFactory;

    @Inject
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private InterceptorChainProcessor dispatcher;

    @Resource
    private WebServiceContext webServiceContext;

    @Inject
    private MDIValidator mdiValidator;

    @WebMethod(action = "http://schemas.cjse.gov.uk/endpoint/wsdl/submit")
    @WebResult(name = "SubmitResponseMes",
            targetNamespace = "http://schemas.cjse.gov.uk/endpoint/types/", partName = "submitResult")
    public SubmitResponse submit(@WebParam(name = "SubmitRequestMes",
            targetNamespace = "http://schemas.cjse.gov.uk/endpoint/types/",
            partName = "SubmitRequestMes") final SubmitRequest submitRequestMes) {

        final SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setRequestID(submitRequestMes.getRequestID());

        final Optional<ValidationError> validationError = mdiValidator.validate(submitRequestMes);

        if (validationError.isPresent()) {
            submitResponse.setResponseCode(validationError.get().getCode());
            submitResponse.setResponseText(validationError.get().getText());
        } else {
            submitResponse.setResponseCode(1);
            submitResponse.setResponseText("Success");
        }

        dispatchSpiProsecutionCase(submitRequestMes, validationError);

        return submitResponse;

    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    private void dispatchSpiProsecutionCase(final SubmitRequest submitRequestMes, final Optional<ValidationError> validationError) {
        final CjseMessage cjseMessage = getCJSEMessageFromRequest(submitRequestMes, validationError);
        final JsonObject cjseMessageJsonObject = objectToJsonObjectConverter.convert(cjseMessage);

        final JsonEnvelope jsonEnvelope = restEnvelopeBuilderFactory.builder()
                .withInitialPayload(Optional.of(cjseMessageJsonObject))
                .withAction("hmcts.cjs.receive-spi-message")
                .build();

        final Metadata metadata = JsonEnvelope.metadataFrom(jsonEnvelope.metadata())
                .withId(randomUUID())
                .withUserId(getUserId())
                .build();

        dispatcher.process(interceptorContextWithInput(envelopeFrom(metadata, jsonEnvelope.payload())));
    }

    private CjseMessage getCJSEMessageFromRequest(final SubmitRequest submitRequestMes, final Optional<ValidationError> validationError) {
        final String submitRequestMesTimestamp = submitRequestMes.getTimestamp() == null ? null : submitRequestMes.getTimestamp().toString();

        return CjseMessage.cjseMessage()
                .withSourceId(submitRequestMes.getSourceID())
                .withDestinationID(submitRequestMes.getDestinationID())
                .withRequestId(submitRequestMes.getRequestID())
                .withExecMode(submitRequestMes.getExecMode() == null ? null : submitRequestMes.getExecMode().value())
                .withTimestamp(submitRequestMesTimestamp)
                .withMessage(submitRequestMes.getMessage())
                .withMdiError(getError(validationError))
                .build();
    }

    @WebMethod(action = "http://schemas.cjse.gov.uk/endpoint/wsdl/retrieve")
    @WebResult(name = "RetrieveResponseMes", targetNamespace = "http://schemas.cjse.gov.uk/endpoint/types/", partName = "retrieveResult")
    public SubmitResponse retrieve(
            @WebParam(name = "RetrieveRequestMes", targetNamespace = "http://schemas.cjse.gov.uk/endpoint/types/", partName = "RetrieveRequestMes") final
            RetrieveRequest retrieveRequestMes) {

        final SubmitResponse submitResponse = new SubmitResponse();
        submitResponse.setRequestID(retrieveRequestMes.getRequestID());
        submitResponse.setResponseCode(309);
        submitResponse.setResponseText("RetrieveNotAvailable");

        return submitResponse;

    }

    private SpiError getError(final Optional<ValidationError> validationErrorOptional) {
        if (validationErrorOptional.isPresent()) {
            final ValidationError validationError = validationErrorOptional.get();
            return SpiError.spiError()
                    .withErrorCode(validationError.getCode())
                    .withErrorDescription(validationError.getText())
                    .build();
        } else {
            return null;
        }
    }

    private String getUserId() {
        @SuppressWarnings({"unchecked", "squid:S00117"}) final Map<String, List<String>> http_headers = (Map<String, List<String>>) webServiceContext.getMessageContext().get(MessageContext.HTTP_REQUEST_HEADERS);
        final String userId = http_headers.get(HeaderConstants.USER_ID).get(0);
        LOGGER.info("UserId from webServiceContext: {}", userId);
        return userId;
    }
}
