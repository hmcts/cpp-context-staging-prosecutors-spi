package uk.gov.moj.cpp.casefilter.azure.function;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.storage.StorageException;
import uk.gov.moj.cpp.casefilter.azure.entity.EjectedOrFilteredCase;
import uk.gov.moj.cpp.casefilter.azure.exception.AzureStorageException;
import uk.gov.moj.cpp.casefilter.azure.pojo.CaseFilterRule;
import uk.gov.moj.cpp.casefilter.azure.pojo.SpiCase;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.PCFQueryService;
import uk.gov.moj.cpp.casefilter.azure.service.StagingProsecutorsSpiCommandService;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpMethod.GET;
import static com.microsoft.azure.functions.HttpMethod.POST;
import static com.microsoft.azure.functions.annotation.AuthorizationLevel.FUNCTION;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.validator.GenericValidator.isDate;
import static uk.gov.moj.cpp.casefilter.azure.pojo.SpiCase.SpiCaseBuilder.aSpiCase;

@SuppressWarnings({"squid:S1312", "squid:S1166", "squid:S2629", "squid:S2677"})
public class ApplyFilterRules {

    private static final String INITIATION_CODE_FOR_SUMMONS = "S";
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final String TIME_PATTERN = "HH:mm:ss";
    private static final String COURT_CENTRE_CODE = "CourtCentreCode";
    private static final String PROSECUTOR_CODE = "ProsecutorCode";
    private static final String INITIATION_CODE = "InitiationCode";
    private static final String CASE_REFERENCE = "CaseReference";
    private static final String DATE_OF_HEARING = "DateOfHearing";
    private static final String TIME_OF_HEARING = "TimeOfHearing";
    private static final String SUMMONS_CODE = "SummonsCode";

    private AzureCloudStorageService azureCloudStorageService;
    private PCFQueryService pcfQueryService;
    private StagingProsecutorsSpiCommandService stagingProsecutorsSpiCommandService;
    private Logger logger = null;

    public ApplyFilterRules() {
        this.azureCloudStorageService = new AzureCloudStorageService();
        this.pcfQueryService = new PCFQueryService();
        this.stagingProsecutorsSpiCommandService = new StagingProsecutorsSpiCommandService();
    }

    @FunctionName("applyFilterRules")
    public HttpResponseMessage applyFilterRules(
            @HttpTrigger(name = "req", methods = {GET, POST}, authLevel = FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        logger = context.getLogger();
        logger.info("Java applyFilterRules processed a request.");

        final SpiCase spiCase = createSpiCase(request);

        if (!validateParams(spiCase)) {
            logger.info("Mandatory parameters missing (CourtCentreCode, ProsecutorCode, InitiationCode, CaseReference, DateOfHearing, TimeOfHearing, SummonsCode) on the query string");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(true).build();
        }

        if (isAlreadyFilteredOut(spiCase.getProsecutorOUCode(), spiCase.getUrn())) {
            return request.createResponseBuilder(HttpStatus.OK).body(true).build();
        }

        logger.info("Case is not filtered nor ejected");

        if (pcfQueryService.isCaseExistsInPCF(spiCase.getUrn(), logger)) {
            logger.info("Case already exists in PCF");
            return request.createResponseBuilder(HttpStatus.OK).body(false).build();
        }

        logger.info("Applying case filter rules");

        final boolean filteredOut = !matchCaseRule(spiCase);
        if (filteredOut) {
            stagingProsecutorsSpiCommandService.filterProsecutionCaseInStagingProsecutorSpi(spiCase.getUrn(),logger);
            updateTable(spiCase);
        }
        return request.createResponseBuilder(HttpStatus.OK).body(filteredOut).build();
    }

    private SpiCase createSpiCase(final HttpRequestMessage<Optional<String>> request) {
        final String courtCentreOUCode = request.getQueryParameters().get(COURT_CENTRE_CODE);
        final String prosecutorOUCode = request.getQueryParameters().get(PROSECUTOR_CODE);
        final String caseInitiationCode = request.getQueryParameters().get(INITIATION_CODE);
        final String caseReference = request.getQueryParameters().get(CASE_REFERENCE);
        final Optional<LocalDate> dateOfHearing = ofNullable(request.getQueryParameters().get(DATE_OF_HEARING)).filter(d -> isDate(d, DATE_PATTERN, true)).map(LocalDate::parse);
        final Optional<LocalTime> timeOfHearing = ofNullable(request.getQueryParameters().get(TIME_OF_HEARING)).map(t -> t.split("\\.")[0]).filter(t -> isDate(t, TIME_PATTERN, true)).map(LocalTime::parse);
        final Optional<String> summonsCode = ofNullable(stripToNull(request.getQueryParameters().get(SUMMONS_CODE)));

        final SpiCase spiCase = aSpiCase()
                .withCourtCentreOUCode(courtCentreOUCode).withProsecutorOUCode(prosecutorOUCode)
                .withCaseInitiationCode(caseInitiationCode).withUrn(caseReference)
                .withHearingDate(dateOfHearing.orElse(null))
                .withHearingTime(timeOfHearing.orElse(null))
                .withSummonsCode(summonsCode)
                .build();

        logger.info(() -> format("Apply filter values, CourtCentreCode: %s, ProsecutorCode: %s, InitiationCode: %s, CaseReference: %s, SummonsCode: %s, DateOfHearing: %s, TimeOfHearing: %s",
                courtCentreOUCode, prosecutorOUCode, caseInitiationCode, caseReference, summonsCode.orElse(EMPTY), dateOfHearing.map(LocalDate::toString).orElse(EMPTY), timeOfHearing.map(LocalTime::toString).orElse(EMPTY)));
        return spiCase;
    }

    private boolean validateParams(final SpiCase spiCase) {
        final List<Supplier<Boolean>> conditions =
                asList(() -> isBlank(spiCase.getCourtCentreOUCode()),
                        () -> isBlank(spiCase.getProsecutorOUCode()),
                        () -> isBlank(spiCase.getCaseInitiationCode()),
                        () -> INITIATION_CODE_FOR_SUMMONS.equals(spiCase.getCaseInitiationCode()) && !spiCase.getSummonsCode().isPresent(),
                        () -> isBlank(spiCase.getUrn()),
                        () -> isNull(spiCase.getHearingDate()));

        return conditions.stream().noneMatch(Supplier::get);
    }

    private boolean isAlreadyFilteredOut(final String prosecutorOUCode, final String caseReference) {
        return azureCloudStorageService.isCaseFilteredOrEjected(prosecutorOUCode, caseReference, logger);
    }

    private boolean matchCaseRule(SpiCase spiCase) {
        try {
            return CaseFilterRule.anyRuleMatches(azureCloudStorageService.readRemoteFile(), spiCase, logger);
        } catch (StorageException e) {
            logger.severe("failed to read file");
            throw new AzureStorageException("Failed to read case filter rules file", e);
        }
    }

    List<CaseFilterRule> getCaseFilters() {
        try {
            return CaseFilterRule.createFilters(azureCloudStorageService.readRemoteFile());

        } catch (StorageException e) {
            logger.severe("failed to read file");
            throw new AzureStorageException("Failed to read case filter rules file", e);
        }
    }


    private void updateTable(final SpiCase spiCase) {

        final EjectedOrFilteredCase ejectedOrFilteredCase = new EjectedOrFilteredCase(spiCase.getProsecutorOUCode(), spiCase.getUrn());
        ejectedOrFilteredCase.setCaseInitiationCode(spiCase.getCaseInitiationCode());
        ejectedOrFilteredCase.setCourtCentreOUCode(spiCase.getCourtCentreOUCode());
        ejectedOrFilteredCase.setCaseReference(spiCase.getUrn());
        ejectedOrFilteredCase.setProsecutorOUCode(spiCase.getProsecutorOUCode());
        ejectedOrFilteredCase.setHearingDateTime(spiCase.getHearingDate(), spiCase.getHearingTime());
        ejectedOrFilteredCase.setSummonsCode(spiCase.getSummonsCode().orElse(EMPTY));
        ejectedOrFilteredCase.setIsFiltered(true);
        ejectedOrFilteredCase.setIsEjected(false);

            logger.info(() -> format("Update table updateEjectedOrFilteredCase for case initiation code= %s", ejectedOrFilteredCase.getCaseInitiationCode()));
        try {
            azureCloudStorageService.createOrUpdateEjectedOrFilteredCase(ejectedOrFilteredCase);
        } catch (StorageException | URISyntaxException | InvalidKeyException e) {
            logger.severe("Failed to update table " + e);
        }
    }

    public void setAzureCloudStorageService(final AzureCloudStorageService azureCloudStorageService) {
        this.azureCloudStorageService = azureCloudStorageService;
    }

    public void setPcfQueryService(final PCFQueryService pcfQueryService) {
        this.pcfQueryService = pcfQueryService;
    }

    public void setStagingProsecutorsSpiCommandService(final StagingProsecutorsSpiCommandService stagingProsecutorsSpiCommandService) {
        this.stagingProsecutorsSpiCommandService = stagingProsecutorsSpiCommandService;
    }
}
