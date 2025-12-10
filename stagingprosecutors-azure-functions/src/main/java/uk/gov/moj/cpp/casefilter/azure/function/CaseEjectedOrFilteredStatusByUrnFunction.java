package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.Optional;
import java.util.logging.Logger;

@SuppressWarnings({"squid:S1312"})
public class CaseEjectedOrFilteredStatusByUrnFunction {

    private static final boolean VALID = true;
    private AzureCloudStorageService azureCloudStorageService;
    private static final String START_TAG = "<";
    private static final String END_TAG = ">";

    private static final String URN = "URN";

    public CaseEjectedOrFilteredStatusByUrnFunction() {
        if (this.azureCloudStorageService == null) {
            this.azureCloudStorageService = new AzureCloudStorageService();
        }
    }

    @FunctionName("isCaseEjectedOrFilteredByUrn")
    public HttpResponseMessage isCaseEjectedOrFilteredByUrn(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        final Logger logger = context.getLogger();

        final String caseReference = parseStreamToGetCaseReference(request.getBody());

        if (isBlank(caseReference)) {
            logger.info("Case Reference not found in the request");
            return request.createResponseBuilder(HttpStatus.OK).body(!VALID).build();
        }

        //check is case ejected or filterd
        final boolean isFilter = azureCloudStorageService.isCaseFilteredOrEjected( caseReference, logger);

        if (isFilter) {
            logger.info("Record is either ejected or filtered");
            return request.createResponseBuilder(HttpStatus.OK).body(VALID).build();
        } else {
            logger.info("Record not found or record is neither ejected nor filtered");
            return request.createResponseBuilder(HttpStatus.OK).body(!VALID).build();
        }
    }

    public void setAzureCloudStorageService(final AzureCloudStorageService azureCloudStorageService) {
        this.azureCloudStorageService = azureCloudStorageService;
    }

    private String parseStreamToGetCaseReference(final Optional<String> body) {
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
}
