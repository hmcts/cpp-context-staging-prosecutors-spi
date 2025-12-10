package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.apache.commons.lang3.StringUtils;
import uk.gov.moj.cpp.casefilter.azure.pojo.ProsecutorForPTIUrn;
import uk.gov.moj.cpp.casefilter.azure.service.RefDataQueryService;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.StringUtils.isBlank;

@SuppressWarnings({"squid:S1312"})
public class GetProsecutorOucodeForPtiUrnFunction {

    private static final boolean VALID = true;
    private static final String START_TAG = "<";
    private static final String END_TAG = ">";
    private static final String URN = "URN";

    private RefDataQueryService refDataQueryService = new RefDataQueryService();

    @FunctionName("getProsecutorOucodeForPtiUrnFunction")
    public HttpResponseMessage getProsecutorOucodeForPtiUrnFunction(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Logger logger = context.getLogger();
        logger.info("Executing getProsecutorForPTIUrnFunction");
        final String ptiUrn = parseStreamToGetPtiUrn(request.getBody());
        if (isBlank(ptiUrn)) {
            logger.info("ptiUrn not found in the request");
            return request.createResponseBuilder(HttpStatus.OK).body(!VALID).build();
        }
        final String ouCodeForPtiUrn;
        try {
             ouCodeForPtiUrn = refDataQueryService.getProsecutorOucodeForPtiUrn(ptiUrn, logger);
        } catch (Exception ex) {
            logger.log(Level.SEVERE,String.format("Error querying RefData for  ptiURN %s , %s", ptiUrn, ex));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                     .body(new ProsecutorForPTIUrn("error")).build();
        }
        if(StringUtils.isBlank(ouCodeForPtiUrn)) {
            return request.createResponseBuilder(HttpStatus.OK).body(!VALID).build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .body(new ProsecutorForPTIUrn(ouCodeForPtiUrn)).build();

    }

    private String parseStreamToGetPtiUrn(final Optional<String> body) {
        String urn = null;
        if (body.isPresent()) {
            final String message = body.get();
            urn = getPropertyValue(message, URN);
        }
        return urn;
    }

    private String getPropertyValue(final String message, final String tagName) {
        final int startTagIndex = message.indexOf(buildStartTagFor(tagName));
        final int closeTagIndex = message.indexOf(buildEndTagFor(tagName));

        if (startTagIndex == -1 || closeTagIndex == -1) {
            return null;
        }
        return message.substring(startTagIndex + (START_TAG + tagName + END_TAG).length(), closeTagIndex);
    }

    private String buildEndTagFor(final String name) {
        return format("%s/%s%s", START_TAG, name, END_TAG);
    }

    private String buildStartTagFor(final String name) {
        return format("%s%s%s", START_TAG, name, END_TAG);
    }

    public void setRefDataQueryService(RefDataQueryService refDataQueryService) {
        this.refDataQueryService = refDataQueryService;
    }
}

