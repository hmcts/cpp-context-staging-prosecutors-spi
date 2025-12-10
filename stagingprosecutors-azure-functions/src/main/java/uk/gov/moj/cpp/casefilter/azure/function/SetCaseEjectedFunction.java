package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.casefilter.azure.entity.EjectedOrFilteredCase;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.EventGridService;
import uk.gov.moj.cpp.casefilter.azure.utils.FileUtil;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Optional;
import java.util.logging.Logger;

import javax.json.JsonObject;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.StorageException;

@SuppressWarnings({"squid:S1312", "squid:S2629", "squid:S3655", "squid:S2629", "squid:S1166"})
public class SetCaseEjectedFunction {

    private static final String CASE_ID = "CaseId";
    private static final String PROSECUTOR_CODE = "ProsecutorCode";
    private static final String INITIATION_CODE = "InitiationCode";
    private static final String CASE_REFERENCE = "CaseReference";
    private static final String SUCCESS = "Case Ejected!";

    private AzureCloudStorageService azureCloudStorageService;

    private EventGridService eventGridService;

    public  SetCaseEjectedFunction() {
        azureCloudStorageService = new AzureCloudStorageService();
        eventGridService = new EventGridService();
    }

    @FunctionName("setCaseEjected")
    public HttpResponseMessage setCaseEjected(
            @HttpTrigger(name = "req", methods = {POST}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        final Logger logger = context.getLogger();

        logger.info(format("Request body is: %s", request.getBody().orElse("")));

        // Check request body
        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Payload not found.")
                    .build();
        }

        final String body = request.getBody().get();
        final JsonObject jsonObj = FileUtil.getJsonObject(body);
        final String caseId = jsonObj.getString(CASE_ID, null);
        final String prosecutorOUCode = jsonObj.getString(PROSECUTOR_CODE, null);
        final String caseInitiationCode = jsonObj.getString(INITIATION_CODE, null);
        final String caseReference = jsonObj.getString(CASE_REFERENCE, null);


        if (isBlank(caseId) || isBlank(prosecutorOUCode) || isBlank(caseInitiationCode) || isBlank(caseReference)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass all parameters (CaseId, ProsecutorOUCode, CaseInitiationCode, CaseReference) with payload").build();
        } else {
            final EjectedOrFilteredCase ejectedOrFilteredCase = createOrUpdateEjectedOrFilteredCase(caseId, prosecutorOUCode, caseInitiationCode, caseReference, logger);
            if (ejectedOrFilteredCase.getIsEjected()) {
                eventGridService.publishEventsToCourtStore(body, logger);
            }
            return request.createResponseBuilder(HttpStatus.OK)
                   .body(SUCCESS)
                   .build();
        }
    }

    private EjectedOrFilteredCase createOrUpdateEjectedOrFilteredCase(final String caseId, final String prosecutorOUCode,
                             final String caseInitiationCode, final String caseReference, Logger logger) {

        final EjectedOrFilteredCase ejectedOrFilteredCase = new EjectedOrFilteredCase(prosecutorOUCode, caseReference);
        final boolean isCaseFiltered = false;
        final boolean isCaseEjected = true;

        ejectedOrFilteredCase.setCaseInitiationCode(caseInitiationCode);
        ejectedOrFilteredCase.setCaseId(caseId);
        ejectedOrFilteredCase.setCaseReference(caseReference);
        ejectedOrFilteredCase.setProsecutorOUCode(prosecutorOUCode);
        ejectedOrFilteredCase.setIsFiltered(isCaseFiltered);
        ejectedOrFilteredCase.setIsEjected(isCaseEjected);

        try {
            logger.info(format("Update table updateEjectedOrFilteredCase = %s", ejectedOrFilteredCase.toString()));
            azureCloudStorageService.createOrUpdateEjectedOrFilteredCase(ejectedOrFilteredCase);
        } catch (StorageException | URISyntaxException | InvalidKeyException e) {
            logger.info("Failed to update table" + e.getMessage());
        }

        return ejectedOrFilteredCase;
    }

    public void setAzureCloudStorageService(final AzureCloudStorageService azureCloudStorageService) {
        this.azureCloudStorageService = azureCloudStorageService;
    }

    public void setEventGridService(EventGridService eventGridService) {
        this.eventGridService = eventGridService;
    }
}
