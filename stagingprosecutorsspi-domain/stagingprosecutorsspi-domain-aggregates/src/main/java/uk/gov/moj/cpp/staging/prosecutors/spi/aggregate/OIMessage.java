package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.UNUSABLE_DATA_STREAM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.ErrorsReportedWithCPPResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiPoliceSystemUpdated;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OperationalDetailsPreparedForResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.SpiResultPreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiError;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.ErrorDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OiValidationErrorsFound;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.OIValidator;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;
import uk.gov.moj.cpp.staging.soap.schema.OIDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("squid:S1948")
public class OIMessage implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(OIMessage.class);

    private static final long serialVersionUID = -7758461059902083696L;

    private String correlationId;
    private String systemId;

    private RouteDataRequestType receivedRequest;
    private boolean asyncResponsePreparedForReceivedRequest;

    @SuppressWarnings("squid:S1450")
    private RouteDataResponseType responseReceivedForSentResponse;
    private boolean resultPreparedForReceivedRequest;


    @SuppressWarnings("squid:S1450")
    private UUID cppMessageIdForResponse;
    private UUID resultMDIId;

    public Stream<Object> oiRequestReceived(final OIDetails oiDetails, final UUID streamId, final OIValidator oiValidator) {

        final RouteDataRequestType routeDataRequestType = oiDetails.getRouteDataRequestType();

        final OIInputVO oiInputVO = new OIInputVO.OIInputVOBuilder().withCorrelationId(this.correlationId).withSystemId(this.systemId).build();

        final List<Optional<ValidationError>> validationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        final ErrorDetails errorDetails = buildErrorDetails(validationErrors);

        if (!errorDetails.getErrorDetail().isEmpty()) {
            final Stream.Builder<Object> builder = Stream.builder();
            final RouteDataResponseType routeDataResponseType = prepareRouteDataForErrors(errorDetails, routeDataRequestType);
            builder.add(OiValidationErrorsFound.oiValidationErrorsFound()
                    .withErrorDetails(errorDetails)
                    .withCorrelationId(routeDataRequestType.getRequestFromSystem().getCorrelationID())
                    .withSystemId(routeDataRequestType.getRequestFromSystem().getSystemID().getValue())
                    .build());
            builder.add(new OperationalDetailsPreparedForResponse(randomUUID(), routeDataResponseType));
            return apply(builder.build());
        } else {

            return apply(of(OiRequestMessageReceived.oiRequestMessageReceived()
                    .withDataStreamContent(oiDetails.getRouteDataRequestType().getDataStream().getDataStreamContent())
                    .withCorrelationId(routeDataRequestType.getRequestFromSystem().getCorrelationID())
                    .withSystemId(routeDataRequestType.getRequestFromSystem().getSystemID().getValue())
                    .withPoliceSystemId(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue())
                    .withOiId(streamId)
                    .withRouteDataRequestType(routeDataRequestType)
                    .build())

            );
        }
    }

    @SuppressWarnings("squid:S00112")
    public Stream<Object> oiResponseReceived(final OIDetails oiDetails) {
        final RouteDataResponseType routeDataResponseType = oiDetails.getRouteDataResponseType();
        final Stream.Builder<Object> builder = Stream.builder();

        if (asyncResponsePreparedForReceivedRequest || resultPreparedForReceivedRequest) {
            builder.add(new ErrorsReportedWithCPPResponse(oiDetails.getMessageId(), routeDataResponseType));
        } else {
            LOGGER.warn("Async response message with no matching correlation found with message id {}", oiDetails.getMessageId());
        }
        return apply(builder.build());
    }

    public Stream<Object> prepareSPIResult(final UUID spiResultId, final SpiResult spiResult, String cppSystemId, UUID correlationId) {

        final RouteDataRequestType routeDataRequestType = new RouteDataRequestBuilder().prepareRouteDataRequest(spiResult, cppSystemId, correlationId);
        return apply(of(new SpiResultPreparedForSending(routeDataRequestType, spiResult.getPayload(), spiResult.getPtiUrn(), spiResultId, randomUUID())));
    }

    public Stream<Object> updatePoliceSystemId(final UUID oiId, final String policeSystemId) {
        return apply(of(new OiPoliceSystemUpdated(oiId, policeSystemId)));
    }


    @SuppressWarnings("pmd:NullAssignment")
    private RouteDataResponseType prepareRouteDataForErrors(final ErrorDetails errorDetails, final RouteDataRequestType routeDataRequestType) {
        final RouteDataResponseType routeDataResponseType = new RouteDataResponseType();
        routeDataResponseType.setResponseToSystem(routeDataRequestType.getRequestFromSystem());
        final String routeId = getRouteId(routeDataRequestType);
        final List<RouteDataResponseType.OperationStatus> operationStatusList = errorDetails.getErrorDetail().stream().map(x -> mapErrorDetails(x, routeId)).collect(Collectors.toList());
        routeDataResponseType.getOperationStatus().addAll(operationStatusList);
        return routeDataResponseType;
    }

    public Stream<Object> prepareOIResponseForXSDFailures(final String errorMessage) {
        final RouteDataResponseType routeDataResponseType = new RouteDataResponseType();
        final String routeId = getRouteId(receivedRequest);
        routeDataResponseType.setResponseToSystem(this.receivedRequest.getRequestFromSystem());
        final RouteDataResponseType.OperationStatus operationStatus = new RouteDataResponseType.OperationStatus();
        operationStatus.setCode(UNUSABLE_DATA_STREAM.getCode());
        operationStatus.setStatusClass(UNUSABLE_DATA_STREAM.getErrorType());
        operationStatus.setResponseContext(errorMessage);
        operationStatus.setDescription(UNUSABLE_DATA_STREAM.getText());
        operationStatus.setRouteId(routeId);
        routeDataResponseType.getOperationStatus().add(operationStatus);
        return apply(of(new OperationalDetailsPreparedForResponse(randomUUID(), routeDataResponseType)));
    }

    @SuppressWarnings({"squid:S3358", "pmd:NullAssignment"})
    private String getRouteId(final RouteDataRequestType routeDataRequestType) {
        return routeDataRequestType.getRoutes() != null ? routeDataRequestType.getRoutes().getRoute() != null && !routeDataRequestType.getRoutes().getRoute().isEmpty() ? routeDataRequestType.getRoutes().getRoute().get(0).getRouteID() : null : null;
    }

    private RouteDataResponseType.OperationStatus mapErrorDetails(final SpiError error, final String routeId) {
        final RouteDataResponseType.OperationStatus operationStatus = new RouteDataResponseType.OperationStatus();
        operationStatus.setCode(error.getErrorCode());
        operationStatus.setStatusClass(error.getErrorType());
        operationStatus.setResponseContext(error.getErrorDescription());
        operationStatus.setRouteId(routeId);
        return operationStatus;
    }

    private ErrorDetails buildErrorDetails(final List<Optional<ValidationError>> validationErrors) {
        final List<SpiError> errors = new ArrayList<>();
        validationErrors.stream().forEach(s -> {
            if (s.isPresent()) {
                final ValidationError validationError = s.get();
                errors.add(new SpiError(validationError.getCode(), validationError.getText(), "errorResponse", validationError.getErrorType()));
            }
        });
        return new ErrorDetails(errors);
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(OiRequestMessageReceived.class)
                        .apply(oiRequestMessageReceived -> {
                            this.correlationId = oiRequestMessageReceived.getCorrelationId();
                            this.systemId = oiRequestMessageReceived.getSystemId();
                            this.receivedRequest = oiRequestMessageReceived.getRouteDataRequestType();
                        }),
                when(OperationalDetailsPreparedForResponse.class).apply(x -> {
                    this.cppMessageIdForResponse = x.getMessageId();
                    this.asyncResponsePreparedForReceivedRequest = true;
                }),
                when(OiValidationErrorsFound.class).apply(oiValidationErrorsFound -> {
                    this.correlationId = oiValidationErrorsFound.getCorrelationId();
                    this.systemId = oiValidationErrorsFound.getSystemId();
                }),
                when(ErrorsReportedWithCPPResponse.class).apply(x -> this.responseReceivedForSentResponse = x.getRouteDataResponseType()),
                when(SpiResultPreparedForSending.class).apply(x ->
                {
                    this.correlationId = x.getSpiResultId().toString();
                    this.resultMDIId = x.getMessageId();
                    this.resultPreparedForReceivedRequest = true;
                }),
                otherwiseDoNothing()
        );
    }

    public UUID getResultMDIId() {
        return resultMDIId;
    }
}
