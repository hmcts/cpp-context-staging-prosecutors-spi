package uk.gov.moj.cpp.staging.prosecutors.spi.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.cjse.schemas.endpoint.types.RetrieveRequest;
import uk.gov.cjse.schemas.endpoint.types.RetrieveResponse;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.cjse.schemas.endpoint.types.SubmitResponse;
import uk.gov.cjse.schemas.endpoint.wsdl.CJSEPort;
import uk.gov.cjse.schemas.endpoint.wsdl.CJSEService;
import uk.gov.justice.services.test.utils.core.rest.RestClient;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import com.google.common.base.Joiner;
import org.apache.commons.lang3.StringUtils;

public class SPISoapAdapterHelper {

    public static final String BASE_URI = System.getProperty("INTEGRATION_HOST_URI", "http://localhost:8080");

    private static final String USER_ID_HEADER = "CJSCPPUID";
    public static final String CJSE_USER_ID = "626aa507-0d5f-4a45-91de-a255aeffc636";

    private static final String CJSE_END_POINT = BASE_URI + "/stagingprosecutorsspi-service/CJSEService";
    private static final String SOAP_XML_CONTENT_TYPE = "application/soap+xml; charset=UTF-8;";
    private static final String PROSECUTION_CASE_FILTER_URL = "/stagingprosecutorsspi-service/command/api/rest/stagingprosecutors-spi";
    private static final String READ_BASE_URL = "/stagingprosecutorsspi-service/query/api/rest/stagingprosecutors-spi";


    public static String getWriteUrl( final String url, final String resource) {
        return Joiner.on("").join(BASE_URI, url, resource);
    }

    private static CJSEPort getSoapAdapter() {
        final CJSEService cjseService = new CJSEService();
        final CJSEPort cjsePort = cjseService.getDelivery();

        BindingProvider bp = (BindingProvider) cjsePort;
        Map<String, Object> requestContext = bp.getRequestContext();

        requestContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, CJSE_END_POINT);
        Map<String, List<String>> requestHeaders = new HashMap<>();
        requestHeaders.put(USER_ID_HEADER, Arrays.asList(CJSE_USER_ID));
        requestContext.put(MessageContext.HTTP_REQUEST_HEADERS, requestHeaders);
        return cjsePort;
    }

    public static SubmitResponse postCommand(final String payload) {
        final Response response =  new RestClient().postCommand(CJSE_END_POINT, SOAP_XML_CONTENT_TYPE, payload, getRequestHeaders());
        final String soapResponse = response.readEntity(String.class);
        return convertToSubmitResponse(soapResponse);
    }



    public static Response postCommandForCaseFilter(final String ptiUrn,
                                                          final String body,
                                                          final String mediaType) {

        return new RestClient().postCommand(getWriteUrl(PROSECUTION_CASE_FILTER_URL,"/spi/case/" + ptiUrn),
                mediaType,
                body,
                getRequestHeaders()
        );
    }

    public static Response postCommandForPoliceSystemIdUpdate(final String oiId,
                                                    final String body ) {

        return new RestClient().postCommand(getWriteUrl(PROSECUTION_CASE_FILTER_URL,"/oi/" + oiId),
                "application/vnd.stagingprosecutorsspi.command.spi-oi-update-police-system-id+json",
                body,
                getRequestHeaders()
        );
    }


    private static MultivaluedMap<String, Object> getRequestHeaders() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.put(USER_ID_HEADER, Arrays.asList(CJSE_USER_ID));
        return headers;
    }

    private static SubmitResponse convertToSubmitResponse(final String soapResponse) {
        final SubmitResponse submitResponse = new SubmitResponse();

        final int responseCode = Integer.parseInt(StringUtils.substringBetween(soapResponse, "<ResponseCode>", "</ResponseCode>"));
        submitResponse.setResponseCode(responseCode);

        final String responseText = StringUtils.substringBetween(soapResponse, "<ResponseText>", "</ResponseText>");
        submitResponse.setResponseText(responseText);

        return submitResponse;
    }

    public static SubmitResponse sendSubmitRequest(SubmitRequest submitRequest) {
        return getSoapAdapter().submit(submitRequest);
    }

    public static RetrieveResponse sendRetrieveRequest(RetrieveRequest retrieveRequest) {
        return getSoapAdapter().retrieve(retrieveRequest);
    }

    public static void validateSubmitResponse(SubmitResponse submitResponse, String expectedRequestID) {
        assertNotNull(submitResponse);
        assertThat("Response message should be present", submitResponse.getRequestID(), is(expectedRequestID));
        assertThat("Response message should be present", submitResponse.getResponseCode(), is(1));
        assertThat("Response message should be present", submitResponse.getResponseText(), is("Success"));
    }
    
    public static void validateSubmitResponseCodeForUnsupportedElement(SubmitResponse submitResponse) {
        assertNotNull(submitResponse);
        assertThat("Response code should be present", submitResponse.getResponseCode(), is(311));
    }

    public static void validateRetrieveResponse(RetrieveResponse retrieveResponse) {
        assertNotNull(retrieveResponse);
        assertThat("Response code should be present", retrieveResponse.getResponseCode(), is(309));
        assertThat("Response message should be present", retrieveResponse.getResponseText(), is("RetrieveNotAvailable"));
    }

    public static void validateSubmitResponseCodeForInvalidSourceId(SubmitResponse submitResponse) {
        assertNotNull(submitResponse);
        assertThat("Response message should be present", submitResponse.getResponseCode(), is(304));
        assertThat("Response message should be present", submitResponse.getResponseText(), is("WrongSource"));
    }

    public static void validateSubmitResponseCodeForInvalidDestinationId(SubmitResponse submitResponse) {
        assertNotNull(submitResponse);
        assertThat("Response message should be present", submitResponse.getResponseCode(), is(305));
        assertThat("Response message should be present", submitResponse.getResponseText(), is("WrongDestination"));
    }

    public static void validateSubmitResponseCodeForInvalidRequestId(SubmitResponse submitResponse) {
        assertNotNull(submitResponse);
        assertThat("Response message should be present", submitResponse.getResponseCode(), is(306));
        assertThat("Response message should be present", submitResponse.getResponseText(), is("InvalidRequestID"));
    }

    public static void validateSubmitResponseCodeForInvalidExecMode(SubmitResponse submitResponse) {
        assertNotNull(submitResponse);
        assertThat("Response message should be present", submitResponse.getResponseCode(), is(307));
        assertThat("Response message should be present", submitResponse.getResponseText(), is("ModeError"));
    }

    public static String getReadUrl(String resource) {
        return Joiner.on("").join(BASE_URI, READ_BASE_URL, resource);
    }
}
