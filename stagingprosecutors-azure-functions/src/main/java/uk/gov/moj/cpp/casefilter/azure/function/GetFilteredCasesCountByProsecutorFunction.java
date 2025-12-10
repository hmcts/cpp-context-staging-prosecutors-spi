package uk.gov.moj.cpp.casefilter.azure.function;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import uk.gov.moj.cpp.casefilter.azure.pojo.FilteredCaseCountByProsecutor;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.Map;
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
import com.microsoft.azure.storage.StorageException;

@SuppressWarnings({"squid:S1312"})
public class GetFilteredCasesCountByProsecutorFunction {

    private AzureCloudStorageService azureCloudStorageService = new AzureCloudStorageService();

    @FunctionName("getFilteredCasesCountByProsecutor")
    public HttpResponseMessage getFilteredCasesCountByProsecutor(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        final Logger logger = context.getLogger();
        try {
            final Map<String, Long> totalFilteredCaseCountByProsecutor = azureCloudStorageService.getTotalFilteredCaseCountByProsecutor();
            final Map<String, Long> dailyFilteredCaseCountByProsecutor = azureCloudStorageService.getDailyFilteredCaseCountByProsecutor();
            return request.createResponseBuilder(HttpStatus.OK)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .body(new FilteredCaseCountByProsecutor(totalFilteredCaseCountByProsecutor, dailyFilteredCaseCountByProsecutor)).build();
        } catch (StorageException e) {
            logger.info(String.format("Failed to get filtered case count by prosecutors : %s", e));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("getFilteredCasesCountByProsecutor failed with exception: " + e.getMessage()).build();
        }
    }
}

