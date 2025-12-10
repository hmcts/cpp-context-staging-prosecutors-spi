package uk.gov.moj.cpp.casefilter.azure.function;

import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

@SuppressWarnings({"squid:S1312"})
public class CaseEjectedOrFilteredStatusFunction {

    private static final boolean VALID = true;
    private AzureCloudStorageService azureCloudStorageService;
    private static final String CASE_REFERENCE = "CaseReference";
    private static final String PROSECUTOR_CODE = "ProsecutorCode";

    public CaseEjectedOrFilteredStatusFunction() {
        if (this.azureCloudStorageService == null) {
            this.azureCloudStorageService = new AzureCloudStorageService();
        }
    }

    @FunctionName("isCaseEjectedOrFiltered")
    public HttpResponseMessage isCaseEjectedOrFiltered(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        final Logger logger = context.getLogger();

        final String prosecutorOUCode = request.getQueryParameters().get(PROSECUTOR_CODE);
        final String caseReference = request.getQueryParameters().get(CASE_REFERENCE);

        if (isBlank(prosecutorOUCode) || isBlank(caseReference)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass all parameters (ProsecutorCode, CaseReference) on the query string").build();
        }

        //check is case ejected or filterd
        final boolean isFilter = azureCloudStorageService.isCaseFilteredOrEjected(prosecutorOUCode, caseReference, logger);

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
}
