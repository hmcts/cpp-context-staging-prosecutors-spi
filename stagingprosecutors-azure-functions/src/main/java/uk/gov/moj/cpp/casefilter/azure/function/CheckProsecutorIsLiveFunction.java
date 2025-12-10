package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpMethod.GET;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;

import uk.gov.moj.cpp.casefilter.azure.exception.AzureStorageException;
import uk.gov.moj.cpp.casefilter.azure.pojo.CaseFilterRule;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.StorageException;

@SuppressWarnings({"squid:S1312", "squid:S1166", "squid:S2629", "squid:S2677"})
public class CheckProsecutorIsLiveFunction {

    private static final String OUCODE = "oucode";

    private AzureCloudStorageService azureCloudStorageService;
    private Logger logger = null;

    public CheckProsecutorIsLiveFunction() {
        this.azureCloudStorageService = new AzureCloudStorageService();
    }

    @FunctionName("checkProsecutorIsLiveFunction")
    public HttpResponseMessage checkProsecutorIsLive(
            @HttpTrigger(name = "req", methods = {GET}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        logger = context.getLogger();
        logger.info("Java checkProsecutorIsLiveFunction processing a request.");
        final String prosecutorOUCODE = request.getQueryParameters().get(OUCODE);
        logger.info("prosecutorOUCODE : " + prosecutorOUCODE);
        final boolean prosecutorIsLive = getCaseFilters().stream().anyMatch(x -> x.matchOUCODE(prosecutorOUCODE, logger));
        logger.info("return value: " + prosecutorIsLive);
        return request.createResponseBuilder(HttpStatus.OK).body(prosecutorIsLive).build();
    }

    private List<CaseFilterRule> getCaseFilters() {
        try {
            return CaseFilterRule.createFilters(azureCloudStorageService.readRemoteFile());

        } catch (StorageException e) {
            logger.severe("failed to read file");
            throw new AzureStorageException("Failed to read case filter rules file", e);
        }
    }

    public void setAzureCloudStorageService(AzureCloudStorageService azureCloudStorageService) {
        this.azureCloudStorageService = azureCloudStorageService;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
