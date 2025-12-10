package uk.gov.moj.cpp.staging.prosecutors.spi.event.processor;


import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.cjse.schemas.endpoint.wsdl.CJSEPort;
import uk.gov.cjse.schemas.endpoint.wsdl.CJSEService;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ReceiveSyncResponse;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ActivitiService;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessagePreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.RetryDelayRequired;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SyncResponse;

import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
public class CPPMessagePreparedProcessor {


    private static final String SPI_COMMAND_RECIEVE_SYNC_RESPONSE = "stagingprosecutorsspi.command.handler.receive-sync-response";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(CPPMessagePreparedProcessor.class.getName());

    @WebServiceRef(value = CJSEService.class)
    private CJSEPort cjsePort;

    @Inject
    @Value(key = "cjse.endpoint", defaultValue = "http://localhost:8080/simulator/CJSE/message")
    private String cjseEndpoint;

    @Inject
    private Sender sender;

    @Inject
    private ActivitiService activitiService;


    @Handles("stagingprosecutorsspi.event.cppmessage-prepared-for-sending")
    public void processCPPMessagePrepared(final Envelope<CPPMessagePreparedForSending> envelope) throws DatatypeConfigurationException {
        final SubmitRequest submitRequest = envelope.payload().getSubmitRequest();
        final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now()));
        submitRequest.setTimestamp(xmlGregorianCalendar);
        logSubmitRequest(submitRequest);
        customiseAddressForService(cjsePort);
        final SubmitResponse submitResponse = cjsePort.submit(submitRequest);
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(SPI_COMMAND_RECIEVE_SYNC_RESPONSE)
                .build();
        final SyncResponse syncResponse = SyncResponse.syncResponse().withResponseCode(submitResponse.getResponseCode())
                .withResponseText(submitResponse.getResponseText())
                .withRequestID(submitRequest.getRequestID())
                .build();
        final ReceiveSyncResponse receiveSyncResponse = new ReceiveSyncResponse(syncResponse);
        final Envelope<ReceiveSyncResponse> receiveSyncResponseEnvelope = envelopeFrom(metadata, receiveSyncResponse);
        sender.send(receiveSyncResponseEnvelope);
    }


    @Handles("stagingprosecutorsspi.event.cppmessage-retry-delay-required")
    public void processTimerDelayRequired(final Envelope<RetryDelayRequired> envelope) {
        final UUID requestId = fromString(envelope.payload().getRequestId());
        activitiService.startTimerProcess(requestId, envelope.metadata());

    }

    private void customiseAddressForService(final CJSEPort cjsePort) {
        final BindingProvider bindingProvider = (BindingProvider) cjsePort;
        Map<String, Object> requestContext = bindingProvider.getRequestContext();
        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, cjseEndpoint);
    }

    private void logSubmitRequest(final SubmitRequest submitRequest) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("SPI OUT XML :{}", submitRequest.getRequestID());
        }
    }
}

