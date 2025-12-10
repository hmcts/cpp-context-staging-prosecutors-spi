package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import uk.gov.cjse.schemas.endpoint.types.ExecMode;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.justice.services.common.util.UtcClock;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class SubmitRequestBuilder {

    public static final String LM01_REQUEST_ID = "LM01";
    public static final String LM02_REQUEST_ID = "LM02";
    public static final String LM03_REQUEST_ID = "LM03";
    public static final String LM04_REQUEST_ID = "LM04";
    public static final String LM05_REQUEST_ID = "LM05";
    public static final String LM06_REQUEST_ID = "LM06";
    public static final String LM07_REQUEST_ID = "LM07";
    public static final String LM08_REQUEST_ID = "LM08";
    public static final String CM03_REQUEST_ID = "CM03";
    public static final String FM03_REQUEST_ID = "FM03";
    public static final String LM04U_REQUEST_ID = "LM04u";
    public static final String CMS_SOAP_ID = "Z00CJSE";
    public static final String CPP_SOAP_ID = "B00LIBRA";

    private String transactionId;
    private String transactionTimestamp;
    private ExecMode execMode;
    private String sourceId;
    private List<String> destinations;
    private String requestId;
    private String message;

    private SubmitRequestBuilder(String transactionId, String transactionTimestamp, ExecMode execMode, String sourceId, List<String> destinations, String requestId, String message) {
        this.transactionId = transactionId;
        this.transactionTimestamp = transactionTimestamp;
        this.execMode = execMode;
        this.sourceId = sourceId;
        this.destinations = destinations;
        this.requestId = requestId;
        this.message = message;
    }

    public static SubmitRequestBuilder aSubmitRequestToCPP(ExecMode execMode, String sourceId, List<String> destinations, String requestId, String message) {
        return new SubmitRequestBuilder(
                UUID.randomUUID().toString().toUpperCase(),
                fromZonedDateTime(new UtcClock().now()).toXMLFormat(),
                ExecMode.ASYNCH,
                CMS_SOAP_ID,
                Arrays.asList(CPP_SOAP_ID),
                requestId,
                message
        );
    }

    public static SubmitRequestBuilder aSubmitRequestToCPP(String requestId, String message) {
        return aSubmitRequestToCPP(UUID.randomUUID().toString().toUpperCase(), requestId, message);
    }

    public static SubmitRequestBuilder aSubmitRequestToCPP(String requestId,ExecMode execMode, String message) {
        return new SubmitRequestBuilder(
                UUID.randomUUID().toString().toUpperCase(),
                fromZonedDateTime(new UtcClock().now()).toXMLFormat(),
                execMode,
                CMS_SOAP_ID,
                Arrays.asList(CPP_SOAP_ID),
                requestId,
                message
        );
    }
    public static SubmitRequestBuilder aSubmitRequestToCPP(String requestId, String message, List<String> destinationId) {
        return new SubmitRequestBuilder(
                UUID.randomUUID().toString().toUpperCase(),
                fromZonedDateTime(new UtcClock().now()).toXMLFormat(),
                ExecMode.ASYNCH,
                CMS_SOAP_ID,
                destinationId,
                requestId,
                message
        );
    }

    public static SubmitRequestBuilder aSubmitRequestToCPP(String transactionId, String requestId, String message) {

        return new SubmitRequestBuilder(
                transactionId,
                fromZonedDateTime(new UtcClock().now()).toXMLFormat(),
                ExecMode.ASYNCH,
                CMS_SOAP_ID,
                Arrays.asList(CPP_SOAP_ID),
                requestId,
                message
        );
    }

    public static SubmitRequestBuilder aSubmitRequestToCPP(C2ITransactionMetadata c2ITransactionMetadata, String message) {
        return new SubmitRequestBuilder(
                c2ITransactionMetadata.getTransactionId(),
                fromZonedDateTime(c2ITransactionMetadata.getTransactionTimestamp()).toXMLFormat(),
                ExecMode.ASYNCH,
                CMS_SOAP_ID,
                Arrays.asList(CPP_SOAP_ID),
                c2ITransactionMetadata.getRequestId(),
                message
        );
    }

    public SubmitRequest build() throws DatatypeConfigurationException {
        this.message = message.replace("TRANSACTION_ID", transactionId);
        this.message = message.replace("TRANSACTION_TIMESTAMP", transactionTimestamp);

        SubmitRequest submitRequest = new SubmitRequest();
        submitRequest.setExecMode(execMode);
        submitRequest.setSourceID(sourceId);
        submitRequest.getDestinationID().addAll(destinations);
        submitRequest.setRequestID(requestId);
        submitRequest.setMessage(message);

        GregorianCalendar gregorianCalendar= new GregorianCalendar();
        gregorianCalendar.setTime(new Date());
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);

        submitRequest.setTimestamp(xmlGregorianCalendar);

        return submitRequest;
    }

    private static XMLGregorianCalendar fromZonedDateTime(ZonedDateTime zonedDateTime) {
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        return xmlGregorianCalendar;
    }
}
