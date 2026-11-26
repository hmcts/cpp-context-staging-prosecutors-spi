package uk.gov.moj.cpp.casefilter.azure.function;

import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.casefilter.azure.service.EventGridService;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtil;

import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

@SuppressWarnings({"squid:S1312","squid:S3655"})

public class RelayCaseOnCPPToCourtStoreFunction {


    private static final boolean VALID = true;
    private static final String CASE_REFERENCE = "CaseReference";



    private EventGridService eventGridService;

    public RelayCaseOnCPPToCourtStoreFunction() {
        this.eventGridService = new EventGridService();
    }

    @FunctionName("RelayCaseOnCPPToCourtStoreFunction")
    public HttpResponseMessage relayCaseOnCPPToCourtStore(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        final Logger logger = context.getLogger();
        logger.info("RelayCaseOnCPPToCourtStoreFunction : function entry");

        // Check request body
        if (request.getBody().isPresent()) {
            final JsonObject jsonObj = FileUtil.getJsonObject(request.getBody().get());
            final String caseReference = jsonObj.getString(CASE_REFERENCE, null);
            if (isBlank(caseReference)) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass CaseReference in request body.").build();
            }
            //publish event to event grid
            eventGridService.publishCaseOnCPPEvents(caseReference, logger);
            logger.info("Record is relayed to event grid, function exit");
            return request.createResponseBuilder(HttpStatus.OK).body(VALID).build();
        } else {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Payload not found.")
                    .build();
        }



    }

    public void setEventGridService(EventGridService eventGridService) {
        this.eventGridService = eventGridService;
    }

}
