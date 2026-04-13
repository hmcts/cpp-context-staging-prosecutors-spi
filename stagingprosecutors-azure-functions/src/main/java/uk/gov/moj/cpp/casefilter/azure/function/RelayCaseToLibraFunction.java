package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static java.lang.String.format;
import static java.lang.System.getenv;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.Optional;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class RelayCaseToLibraFunction {

    private static final String CJSE_LIBRA_DESTINATION_ID = "cjse-libra-destination-id";
    private static final String DESTINATION_ID_TAG = "NS2:DestinationID";
    private static final String CJSE_DESTINATION_ID = "Z00CJSE";
    private static final String SOURCE_ID_TAG = "NS2:SourceID";
    private static final String CJSE_SOURCE_ID = "C00CommonPlatform";


    @FunctionName("RelayCaseToLibraFunction")
    @SuppressWarnings({"squid:S3457", "squid:S2629", "squid:S1312"})
    public HttpResponseMessage relayCaseToLibra(
            @HttpTrigger(name = "req", methods = {POST}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Optional<String> requestBody = request.getBody();
        if (requestBody.isPresent() && validateMessage(requestBody.get())) {
            final String spiInMessage = requestBody.get();
            final String newDestinationValue = format("<%s>%s</%s>", DESTINATION_ID_TAG, getCjseDestinationValue(), DESTINATION_ID_TAG);
            String newMessage = spiInMessage.replaceAll("<" + DESTINATION_ID_TAG + ">.*<\\/" + DESTINATION_ID_TAG + ">", newDestinationValue);
            final String newSourceValue = format("<%s>%s</%s>", SOURCE_ID_TAG, CJSE_SOURCE_ID , SOURCE_ID_TAG);
            newMessage = newMessage.replaceAll("<" + SOURCE_ID_TAG + ">.*<\\/" + SOURCE_ID_TAG + ">", newSourceValue);
            //doing it separately to make it explicit
            final String newRouteSourceSystem = format("&lt;RouteSourceSystem literalvalue=\"String\"&gt;%s&lt;/RouteSourceSystem&gt;", CJSE_SOURCE_ID);
            newMessage = newMessage.replaceAll("&lt;RouteSourceSystem" +  ".*" + "&lt;/RouteSourceSystem&gt;", newRouteSourceSystem);
            return request.createResponseBuilder(OK).body(newMessage).build();
        }

        return request.createResponseBuilder(BAD_REQUEST).body(getFailureResponse()).build();
    }

    private String getFailureResponse() {
        return createObjectBuilder().add("mdifailure", true).build().toString();
    }

    private String getCjseDestinationValue() {
        final String cjseDestinationPropertyValue = getenv(CJSE_LIBRA_DESTINATION_ID);
        return isNotBlank(cjseDestinationPropertyValue) ? cjseDestinationPropertyValue : CJSE_DESTINATION_ID;
    }

    private boolean validateMessage(final String message) {
        return isNotBlank(message) && message.contains(format("<%s>", DESTINATION_ID_TAG)) && message.contains(format("<%s>", SOURCE_ID_TAG));
    }
}
