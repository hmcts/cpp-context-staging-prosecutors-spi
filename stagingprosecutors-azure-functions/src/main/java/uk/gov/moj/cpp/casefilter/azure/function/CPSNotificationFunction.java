package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.HttpStatus.valueOf;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static uk.gov.moj.cpp.casefilter.azure.utils.ExceptionProvider.generateMissingFieldException;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getPathValue;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getJsonObject;

import uk.gov.moj.cpp.casefilter.azure.pojo.ResponseDto;
import uk.gov.moj.cpp.casefilter.azure.service.CpsPayloadTransformService;
import uk.gov.moj.cpp.casefilter.azure.service.QueryMaterialAndSendNotificationService;

import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class CPSNotificationFunction {

    private QueryMaterialAndSendNotificationService queryMaterialAndSendNotificationService;
    private CpsPayloadTransformService cpsPayloadTransformService;

    public CPSNotificationFunction() {
        this.queryMaterialAndSendNotificationService = new QueryMaterialAndSendNotificationService();
        this.cpsPayloadTransformService = new CpsPayloadTransformService();
    }

    @FunctionName("CPSNotificationFunction")
    @SuppressWarnings({"squid:S1312"})
    public HttpResponseMessage cpsNotification(
            @HttpTrigger(name = "req", methods = {POST}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Logger logger = context.getLogger();
        final Optional<String> postPayload = request.getBody();

        final JsonObject sourcePayload = getJsonObject(postPayload.orElseThrow(() -> new RuntimeException("Message body not found")));
        final String materialId = getPathValue(sourcePayload, "subjectDetails.material").orElseThrow(generateMissingFieldException("subjectDetails.material"));
        final String fileName = getPathValue(sourcePayload, "subjectDetails.fileName").orElseThrow(generateMissingFieldException("subjectDetails.fileName"));
        final JsonObject transformedPayload = transformJsonPayload(sourcePayload);
        final ResponseDto responseDto = queryMaterialAndSendNotificationService.getMaterialById(materialId, fileName, transformedPayload, logger);

        return request.createResponseBuilder(valueOf(responseDto.getStatusCode())).body(responseDto.getMessageBody()).build();
    }

    private JsonObject transformJsonPayload(final JsonObject sourcePayload) {
        return cpsPayloadTransformService.transform(sourcePayload);
    }
}
