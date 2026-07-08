package uk.gov.moj.cpp.casefilter.azure.function;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.casefilter.azure.entity.EjectedOrFilteredCase;
import uk.gov.moj.cpp.casefilter.azure.exception.AzureStorageException;
import uk.gov.moj.cpp.casefilter.azure.pojo.CaseFilterRule;
import uk.gov.moj.cpp.casefilter.azure.service.AzureCloudStorageService;
import uk.gov.moj.cpp.casefilter.azure.service.PCFQueryService;
import uk.gov.moj.cpp.casefilter.azure.service.StagingProsecutorsSpiCommandService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.mockito.ArgumentCaptor;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

public class ApplyFilterRulesTest {

    public static final String A_CASE_REFERENCE = "ABCDEF22";
    public static final boolean FILTERED_IN = false;
    public static final boolean FILTERED_OUT = true;
    private static final String COURT_CENTRE_CODE = "CourtCentreCode";
    private static final String PROSECUTOR_CODE = "ProsecutorCode";
    private static final String INITIATION_CODE = "InitiationCode";
    private static final String CASE_REFERENCE = "CaseReference";
    private static final String DATE_OF_HEARING = "DateOfHearing";
    private static final String TIME_OF_HEARING = "TimeOfHearing";
    private static final String SUMMONS_CODE = "SummonsCode";
    private ApplyFilterRules applyFilterRules;
    private AzureCloudStorageService azureCloudStorageService;
    private PCFQueryService pcfQueryService;
    private StagingProsecutorsSpiCommandService stagingProsecutorsSpiCommandService;
    private boolean caseExistsInPCF = true;
    private ExecutionContext context;
    private HttpRequestMessage<Optional<String>> req;
    private HashMap<Object, Object> queryParams;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setup() throws StorageException {
        applyFilterRules = new ApplyFilterRules();
        stagingProsecutorsSpiCommandService = mock(StagingProsecutorsSpiCommandService.class);
        azureCloudStorageService = mock(AzureCloudStorageService.class);
        pcfQueryService = mock(PCFQueryService.class);
        applyFilterRules.setAzureCloudStorageService(azureCloudStorageService);
        applyFilterRules.setPcfQueryService(pcfQueryService);
        applyFilterRules.setStagingProsecutorsSpiCommandService(stagingProsecutorsSpiCommandService);
        applyFilterRules.setCourtCentreFilterCheckEnabled(true);
        context = mock(ExecutionContext.class);
        final Logger logger = mock(Logger.class);
        when(context.getLogger()).thenReturn(logger);
        doReturn(readFileAsStream("liveCourtsFile.csv")).when(azureCloudStorageService).readRemoteFile();
        doNothing().when(stagingProsecutorsSpiCommandService).filterProsecutionCaseInStagingProsecutorSpi(any(),any());
        req = mock(HttpRequestMessage.class);
        queryParams = new HashMap<>();
        queryParams.put(DATE_OF_HEARING, "2020-03-11");
        queryParams.put(TIME_OF_HEARING, "09:01:01.0000000-00:00");
    }

    @Test
    public void shouldFilterInWhenMandatoryFieldsMatchAnyRules() {
        queryParams.put(COURT_CENTRE_CODE, "B45MH02");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterInWhenCourtRoomIsNotMandatory() {
        queryParams.put(COURT_CENTRE_CODE, "B99MH02");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterInUsingFullProsecutorCodeMatchWhenProsecutorStartsWithAlpha() {
        queryParams.put(COURT_CENTRE_CODE, "B99MH02");
        queryParams.put(PROSECUTOR_CODE, "A45AA00");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterOutUsingFullProsecutorCodeDoesntMatchWhenProsecutorStartsWithAlpha() {
        queryParams.put(COURT_CENTRE_CODE, "B99MH02");
        queryParams.put(PROSECUTOR_CODE, "A45AA99");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterOutWhenProsecutorCodeInCsvFileAndProsecutorCodeInSpiCaseBothDontStartsWithNumber() {
        queryParams.put(COURT_CENTRE_CODE, "B01LY00");
        queryParams.put(PROSECUTOR_CODE, "123AA00");
        queryParams.put(INITIATION_CODE, "C");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterOutAndReturnBadRequestWhenInitiationCodeIsSummonsAndSummonsCodeIsMissing() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B55KL02");
        queryParams.put(PROSECUTOR_CODE, "055CH00");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(CASE_REFERENCE, "55CH0018422");
        queryParams.put(SUMMONS_CODE, "");
        queryParams.put(DATE_OF_HEARING, "2022-01-17");
        queryParams.put(TIME_OF_HEARING, "");
        validateBadRequest(FILTERED_OUT);
    }

    @Test
    public void shouldFilterOutAndReturnBadRequestWhenInitiationCodeIsSummonsAndSummonsCodeIsNewSummons() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B55KL03");
        queryParams.put(PROSECUTOR_CODE, "055CH00");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(CASE_REFERENCE, "55CH0018422");
        queryParams.put(SUMMONS_CODE, "");
        queryParams.put(DATE_OF_HEARING, "2022-01-17");
        queryParams.put(TIME_OF_HEARING, "A");
        validateBadRequest(FILTERED_OUT);
    }

    @Test
    public void shouldFilterOutWhenCourtRoomIsMandatory() {
        queryParams.put(COURT_CENTRE_CODE, "B45MH03");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterInDateOfHearing15March2020FilterRuleWhenDateOfHearingIsOnThatDate() {
        queryParams.put(DATE_OF_HEARING, "2020-03-15");
        queryParams.put(COURT_CENTRE_CODE, "B452X");
        queryParams.put(TIME_OF_HEARING, "09:01:01");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterOutTimeOfHearingWhenItIsBeforeThatTime() {
        queryParams.put(COURT_CENTRE_CODE, "B953X");
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(DATE_OF_HEARING, "2020-03-15");
        queryParams.put(TIME_OF_HEARING, "10:29:59");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterInDateOfHearing15March2020FilterRuleWhenDateOfHearingIsAfterThatDate() {
        queryParams.put(DATE_OF_HEARING, "2020-03-23");
        queryParams.put(COURT_CENTRE_CODE, "B452X");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterOutDateOfHearing14March2020FilterRuleWhenDateOfHearingIsBeforeThatDate() {
        queryParams.put(DATE_OF_HEARING, "2020-03-14");
        queryParams.put(COURT_CENTRE_CODE, "B452X");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterInDateOfHearingAndTime15March2020_10_30FilterRuleWhenTheTimeIsAfterThatRule() {
        queryParams.put(TIME_OF_HEARING, "10:30:01.001");
        queryParams.put(DATE_OF_HEARING, "2020-03-15");
        queryParams.put(COURT_CENTRE_CODE, "B453X");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterInDateOfHearingAndTime15March2020_10_30FilterRuleWhenTheTimeIsEqualToThatRule() {
        queryParams.put(TIME_OF_HEARING, "10:30:00.000");
        queryParams.put(DATE_OF_HEARING, "2020-03-15");
        queryParams.put(COURT_CENTRE_CODE, "B453X");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterInSummonsCodeWhenSummonCodeFilterRuleMatches() {
        queryParams.put(COURT_CENTRE_CODE, "B454X");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(SUMMONS_CODE, "C");
        validate(FILTERED_IN);
    }


    @Test
    public void shouldFilterOutSummonsCodeWhenSummonCodeFilterRuleDoesNotMatches() {
        queryParams.put(COURT_CENTRE_CODE, "B454X");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(SUMMONS_CODE, "V");
        validate(FILTERED_OUT);
    }

    @Test
    public void testApplyFilterRulesShouldReturnBadRequestWhenSummonsCodeIsInvalid() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B454X");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(SUMMONS_CODE, "");
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        validateBadRequest(FILTERED_OUT);
    }

    @Test
    public void testApplyFilterRulesShouldReturnBadRequestWhenGivenAnInvalidDate() throws Exception {
        queryParams.put(DATE_OF_HEARING, "2020-03-DD");
        queryParams.put(COURT_CENTRE_CODE, "B454X");
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(SUMMONS_CODE, "V");
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        validateBadRequest(FILTERED_OUT);
    }

    @Test
    public void shouldFilterInWhenUrnFilterRuleDoesNotMatches() {
        queryParams.put(COURT_CENTRE_CODE, "B455X");
        queryParams.put(CASE_REFERENCE, "TVL12345");
        validate(FILTERED_IN);
    }

    @Test
    public void shouldFilterOutWhenUrnFilterRuleDoesNotMatches() {
        queryParams.put(COURT_CENTRE_CODE, "B455X");
        queryParams.put(CASE_REFERENCE, "TVL12345789");
        validate(FILTERED_OUT);
    }

    @Test
    public void shouldFilterOutWhenAlreadyFilteredOut() {
        queryParams.put(COURT_CENTRE_CODE, "B455X");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(INITIATION_CODE, "C");
        when(azureCloudStorageService.isCaseFilteredOrEjected(
                eq(queryParams.get(PROSECUTOR_CODE).toString()),
                eq(queryParams.get(CASE_REFERENCE).toString()),
                any())).thenReturn(true);
        doReturn(queryParams).when(req).getQueryParameters();
        doRequest(req);
        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        verifyNoMoreInteractions(pcfQueryService);
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
    }

    private void validateBadRequest(boolean filterFlag) throws StorageException {
        doReturn(queryParams).when(req).getQueryParameters();

        doRequest(req);

        doReturn(readFileAsStream("liveCourtsFile.csv")).when(azureCloudStorageService).readRemoteFile();
        when(pcfQueryService.isCaseExistsInPCF(any(), any())).thenReturn(!caseExistsInPCF);

        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        // Verify
        assertThat(ret.getStatus(), is(BAD_REQUEST));
        assertThat(ret.getBody(), is(filterFlag));
    }


    private void validate(boolean filterFlag) {
        queryParams.putIfAbsent(PROSECUTOR_CODE, "523");
        queryParams.putIfAbsent(INITIATION_CODE, "C");
        queryParams.putIfAbsent(CASE_REFERENCE, A_CASE_REFERENCE);

        doReturn(queryParams).when(req).getQueryParameters();

        doRequest(req);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(!caseExistsInPCF);
        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);
        // Verify
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(filterFlag));
    }

    @Test
    public void testApplyFilterRules() {

        queryParams.put(COURT_CENTRE_CODE, "B45MH02");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        doReturn(queryParams).when(req).getQueryParameters();

        doRequest(req);

        when(pcfQueryService.isCaseExistsInPCF(any(), any())).thenReturn(!caseExistsInPCF);

        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        // Verify
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
    }

    @Test
    public void testApplyFilterRulesWhenIsLiveIsNo() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B456X");
        queryParams.put(PROSECUTOR_CODE, "303");
        queryParams.put(INITIATION_CODE, "J");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        doReturn(queryParams).when(req).getQueryParameters();


        doRequest(req);

        when(pcfQueryService.isCaseExistsInPCF(any(), any())).thenReturn(!caseExistsInPCF);

        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        // Verify
        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
    }

    @Test
    public void testApplyFilterRulesWhenNotFiltering() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "1231");
        queryParams.put(PROSECUTOR_CODE, "201");
        queryParams.put(INITIATION_CODE, "CC");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        doReturn(queryParams).when(req).getQueryParameters();


        doRequest(req);

        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(!caseExistsInPCF);

        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        // Verify
        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
    }

    @Test
    public void testApplyFilterRulesShouldReturnBadRequestWhenParametersWereMissing() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "123");
        queryParams.put(PROSECUTOR_CODE, "A203");
        validateBadRequest(FILTERED_OUT);
    }

    @Test
    public void testApplyFilterRulesWhenCaseExistsInPCF() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B456X");
        queryParams.put(PROSECUTOR_CODE, "303");
        queryParams.put(INITIATION_CODE, "J");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        doReturn(queryParams).when(req).getQueryParameters();


        doRequest(req);

        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(caseExistsInPCF);

        // Invoke
        final HttpResponseMessage ret = applyFilterRules.applyFilterRules(req, context);

        // Verify
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
    }

    @Test
    public void shouldFilterInWithoutAnyLookupWhenProsecutorCodeBlankAndToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        for (final String blankProsecutorCode : new String[]{null, "", "   "}) {
            resetQueryParamsToValidRequest();
            queryParams.remove(PROSECUTOR_CODE);
            if (blankProsecutorCode != null) {
                queryParams.put(PROSECUTOR_CODE, blankProsecutorCode);
            }
            final HttpResponseMessage ret = invokeFilter();
            assertThat("ProsecutorCode=[" + blankProsecutorCode + "]", ret.getStatus(), is(OK));
            assertThat("ProsecutorCode=[" + blankProsecutorCode + "]", ret.getBody(), is(FILTERED_IN));
        }
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldFilterInWithoutAnyLookupWhenCaseReferenceBlankAndToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        for (final String blankCaseReference : new String[]{null, "", "  "}) {
            resetQueryParamsToValidRequest();
            queryParams.remove(CASE_REFERENCE);
            if (blankCaseReference != null) {
                queryParams.put(CASE_REFERENCE, blankCaseReference);
            }
            final HttpResponseMessage ret = invokeFilter();
            assertThat("CaseReference=[" + blankCaseReference + "]", ret.getStatus(), is(OK));
            assertThat("CaseReference=[" + blankCaseReference + "]", ret.getBody(), is(FILTERED_IN));
        }
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldFilterInWithoutAnyLookupWhenBothProsecutorCodeAndCaseReferenceBlankAndToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        resetQueryParamsToValidRequest();
        queryParams.remove(PROSECUTOR_CODE);
        queryParams.remove(CASE_REFERENCE);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldNotReturnBadRequestWhenOtherMandatoryParamsAbsentAndToggleOffAndTableMiss() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        queryParams.clear();
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService).isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldFilterOutWhenTableHitAndToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(true);

        queryParams.clear();
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        final HttpResponseMessage minimalRequestResponse = invokeFilter();
        assertThat(minimalRequestResponse.getStatus(), is(OK));
        assertThat(minimalRequestResponse.getBody(), is(FILTERED_OUT));

        resetQueryParamsToValidRequest();
        final HttpResponseMessage fullRequestResponse = invokeFilter();
        assertThat(fullRequestResponse.getStatus(), is(OK));
        assertThat(fullRequestResponse.getBody(), is(FILTERED_OUT));

        verify(azureCloudStorageService, times(2)).isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldFilterInIgnoringPcfAndRulesWhenTableMissAndToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        queryParams.put(COURT_CENTRE_CODE, "1231");
        queryParams.put(PROSECUTOR_CODE, "201");
        queryParams.put(INITIATION_CODE, "CC");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("201"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(true);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(pcfQueryService, never()).isCaseExistsInPCF(any(), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldNotRejectSummonsWithoutSummonsCodeWhenToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        resetQueryParamsToValidRequest();
        queryParams.put(INITIATION_CODE, "S");
        queryParams.put(SUMMONS_CODE, "  ");
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService).isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldNotRejectInvalidHearingDateOrTimeWhenToggleOff() throws Exception {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        resetQueryParamsToValidRequest();
        queryParams.put(DATE_OF_HEARING, "2020-03-DD");
        queryParams.put(TIME_OF_HEARING, "A");
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService, never()).readRemoteFile();
    }

    @Test
    public void shouldPropagateStorageExceptionWhenTableLookupFailsAndToggleOff() {
        applyFilterRules.setCourtCentreFilterCheckEnabled(false);
        resetQueryParamsToValidRequest();
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any()))
                .thenThrow(new AzureStorageException("storage unavailable", new RuntimeException()));
        doReturn(queryParams).when(req).getQueryParameters();
        doRequest(req);
        assertThrows(AzureStorageException.class, () -> applyFilterRules.applyFilterRules(req, context));
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldDefaultToToggleOffWhenEnvVarAbsent() throws Exception {
        assumeTrue(System.getenv("ENABLE_COURT_CENTRE_FILTER_CHECK") == null);
        final ApplyFilterRules defaultConfiguredFilter = new ApplyFilterRules();
        defaultConfiguredFilter.setAzureCloudStorageService(azureCloudStorageService);
        defaultConfiguredFilter.setPcfQueryService(pcfQueryService);
        defaultConfiguredFilter.setStagingProsecutorsSpiCommandService(stagingProsecutorsSpiCommandService);
        queryParams.clear();
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(true);
        doReturn(queryParams).when(req).getQueryParameters();
        doRequest(req);
        final HttpResponseMessage ret = defaultConfiguredFilter.applyFilterRules(req, context);
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService).isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any());
        verify(pcfQueryService, never()).isCaseExistsInPCF(any(), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldReturnBadRequestWhenProsecutorCodeMissingAndToggleOn() throws Exception {
        for (final String blankProsecutorCode : new String[]{null, "", "   "}) {
            resetQueryParamsToValidRequest();
            queryParams.remove(PROSECUTOR_CODE);
            if (blankProsecutorCode != null) {
                queryParams.put(PROSECUTOR_CODE, blankProsecutorCode);
            }
            final HttpResponseMessage ret = invokeFilter();
            assertThat("ProsecutorCode=[" + blankProsecutorCode + "]", ret.getStatus(), is(BAD_REQUEST));
            assertThat("ProsecutorCode=[" + blankProsecutorCode + "]", ret.getBody(), is(FILTERED_OUT));
        }
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldReturnBadRequestWhenCaseReferenceMissingAndToggleOn() throws Exception {
        for (final String blankCaseReference : new String[]{null, "", "   "}) {
            resetQueryParamsToValidRequest();
            queryParams.remove(CASE_REFERENCE);
            if (blankCaseReference != null) {
                queryParams.put(CASE_REFERENCE, blankCaseReference);
            }
            final HttpResponseMessage ret = invokeFilter();
            assertThat("CaseReference=[" + blankCaseReference + "]", ret.getStatus(), is(BAD_REQUEST));
            assertThat("CaseReference=[" + blankCaseReference + "]", ret.getBody(), is(FILTERED_OUT));
        }
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldReturnBadRequestWhenOtherMandatoryParamMissingAndToggleOn() throws Exception {
        final String[][] variants = {
                {COURT_CENTRE_CODE, null},
                {INITIATION_CODE, null},
                {DATE_OF_HEARING, null},
                {DATE_OF_HEARING, "2020-03-DD"}};
        for (final String[] variant : variants) {
            resetQueryParamsToValidRequest();
            queryParams.remove(variant[0]);
            if (variant[1] != null) {
                queryParams.put(variant[0], variant[1]);
            }
            final HttpResponseMessage ret = invokeFilter();
            assertThat(variant[0] + "=[" + variant[1] + "]", ret.getStatus(), is(BAD_REQUEST));
            assertThat(variant[0] + "=[" + variant[1] + "]", ret.getBody(), is(FILTERED_OUT));
        }
        verify(azureCloudStorageService, never()).isCaseFilteredOrEjected(any(), any(), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldShortCircuitWhenCaseAlreadyFilteredOrEjectedAndToggleOn() throws Exception {
        resetQueryParamsToValidRequest();
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(true);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
        verify(azureCloudStorageService).isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(pcfQueryService, stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldReturnFilteredInWithoutRulesWhenCaseExistsInPCFAndToggleOn() throws Exception {
        resetQueryParamsToValidRequest();
        when(azureCloudStorageService.isCaseFilteredOrEjected(eq("523"), eq(A_CASE_REFERENCE), any())).thenReturn(false);
        when(pcfQueryService.isCaseExistsInPCF(eq(A_CASE_REFERENCE), any())).thenReturn(true);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(pcfQueryService).isCaseExistsInPCF(eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService, never()).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldReturnFilteredInWhenRuleMatchesAndToggleOn() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B45MH02");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
        verify(azureCloudStorageService).readRemoteFile();
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
        verifyNoInteractions(stagingProsecutorsSpiCommandService);
    }

    @Test
    public void shouldRunFullLegacyPipelineWhenNoRuleMatchesAndToggleOn() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B456X");
        queryParams.put(PROSECUTOR_CODE, "303");
        queryParams.put(INITIATION_CODE, "J");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
        verify(azureCloudStorageService).isCaseFilteredOrEjected(eq("303"), eq(A_CASE_REFERENCE), any());
        verify(pcfQueryService).isCaseExistsInPCF(eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService).readRemoteFile();
        verify(stagingProsecutorsSpiCommandService).filterProsecutionCaseInStagingProsecutorSpi(eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
    }

    @Test
    public void shouldWriteCorrectEjectedOrFilteredCaseFieldsWhenFilteredOutAndToggleOn() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B456X");
        queryParams.put(PROSECUTOR_CODE, "303");
        queryParams.put(INITIATION_CODE, "J");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getBody(), is(FILTERED_OUT));
        final ArgumentCaptor<EjectedOrFilteredCase> captor = ArgumentCaptor.forClass(EjectedOrFilteredCase.class);
        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(captor.capture());
        final EjectedOrFilteredCase written = captor.getValue();
        assertThat(written.getProsecutorOUCode(), is("303"));
        assertThat(written.getCaseReference(), is(A_CASE_REFERENCE));
        assertThat(written.getCourtCentreOUCode(), is("B456X"));
        assertThat(written.getCaseInitiationCode(), is("J"));
        assertThat(written.getSummonsCode(), is(""));
        assertThat(written.getIsFiltered(), is(true));
        assertThat(written.getIsEjected(), is(false));
        assertThat(written.getHearingDate(), is(notNullValue()));
    }

    @Test
    public void shouldPropagateAzureStorageExceptionWhenRulesFileReadFailsAndToggleOn() throws Exception {
        resetQueryParamsToValidRequest();
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        doThrow(mock(StorageException.class)).when(azureCloudStorageService).readRemoteFile();
        doReturn(queryParams).when(req).getQueryParameters();
        doRequest(req);
        assertThrows(AzureStorageException.class, () -> applyFilterRules.applyFilterRules(req, context));
        verify(stagingProsecutorsSpiCommandService, never()).filterProsecutionCaseInStagingProsecutorSpi(any(), any());
        verify(azureCloudStorageService, never()).createOrUpdateEjectedOrFilteredCase(any());
    }

    @Test
    public void shouldStillReturnFilteredOutWhenUpdateTableWriteFailsAndToggleOn() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B456X");
        queryParams.put(PROSECUTOR_CODE, "303");
        queryParams.put(INITIATION_CODE, "J");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        doThrow(new URISyntaxException("table", "unreachable")).when(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
        verify(stagingProsecutorsSpiCommandService).filterProsecutionCaseInStagingProsecutorSpi(eq(A_CASE_REFERENCE), any());
    }

    @Test
    public void shouldFilterOutInsteadOfThrowingWhenRequestProsecutorCodeShorterThanThreeCharsAndToggleOn() throws Exception {
        queryParams.put(COURT_CENTRE_CODE, "B45MH02");
        queryParams.put(PROSECUTOR_CODE, "20");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_OUT));
        verify(stagingProsecutorsSpiCommandService).filterProsecutionCaseInStagingProsecutorSpi(eq(A_CASE_REFERENCE), any());
        verify(azureCloudStorageService).createOrUpdateEjectedOrFilteredCase(any());
    }

    @Test
    public void shouldNotThrowWhenCsvContainsShortDigitProsecutorRuleCodeAndToggleOn() throws Exception {
        final String rules = "CourtCentreCode,CourtRoom,ProsecutorCode,InitiationCode,SummonsCode,Urn,DateOfHearing,TimeOfHearing,IsLive\n"
                + "B45MH,02,20,C,,,,,Yes\n"
                + "B45MH,02,203,C,,,,,Yes";
        addCaseFilterRuleMock(rules);
        queryParams.put(COURT_CENTRE_CODE, "B45MH02");
        queryParams.put(PROSECUTOR_CODE, "203");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
        when(pcfQueryService.isCaseExistsInPCF(anyString(), any())).thenReturn(false);
        final HttpResponseMessage ret = invokeFilter();
        assertThat(ret.getStatus(), is(OK));
        assertThat(ret.getBody(), is(FILTERED_IN));
    }

    private void resetQueryParamsToValidRequest() {
        queryParams.clear();
        queryParams.put(COURT_CENTRE_CODE, "B455X");
        queryParams.put(PROSECUTOR_CODE, "523");
        queryParams.put(INITIATION_CODE, "C");
        queryParams.put(CASE_REFERENCE, A_CASE_REFERENCE);
        queryParams.put(DATE_OF_HEARING, "2020-03-11");
        queryParams.put(TIME_OF_HEARING, "09:01:01.001");
    }

    private HttpResponseMessage invokeFilter() {
        doReturn(queryParams).when(req).getQueryParameters();
        doRequest(req);
        return applyFilterRules.applyFilterRules(req, context);
    }

    @Test
    public void shouldSkipRuleIfLineIsEmpty() throws StorageException {
        String rule = "CourtCentreCode,ProsecutorCode,InitiationCode,SummonsCode,Urn,DateOfHearing,TimeOfHearing,IsLive\n\n123,02,20,C,,,,,Yes";
        verifyFilterRuleSizeIsEqualToOne(rule);
    }

    @Test
    public void shouldSkipRuleIfLineHasLessThanEightCommas() throws StorageException {
        String rule = "CourtCentreCode,ProsecutorCode,InitiationCode,SummonsCode,Urn,DateOfHearing,TimeOfHearing,IsLive\n123,33\n123,02,20,C,,,,,Yes";
        verifyFilterRuleSizeIsEqualToOne(rule);
    }

    private void verifyFilterRuleSizeIsEqualToOne(final String rule) throws StorageException {
        addCaseFilterRuleMock(rule);

        final List<CaseFilterRule> caseFilterRules = applyFilterRules.getCaseFilters();
        assertThat(caseFilterRules, hasSize(1));
    }

    private void addCaseFilterRuleMock(final String rule) throws StorageException {
        InputStream targetStream = new ByteArrayInputStream(rule.getBytes());
        doReturn(targetStream).when(azureCloudStorageService).readRemoteFile();
    }

    public InputStream readFileAsStream(final String fileName) {
        return this.getClass().getClassLoader().getResourceAsStream(fileName);
    }

    private void doRequest(final HttpRequestMessage<Optional<String>> req) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(req).createResponseBuilder(any(HttpStatus.class));
    }
}
