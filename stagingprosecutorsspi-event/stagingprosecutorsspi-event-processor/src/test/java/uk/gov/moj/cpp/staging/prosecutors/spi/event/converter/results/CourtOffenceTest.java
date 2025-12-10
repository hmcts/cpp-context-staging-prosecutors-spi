package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.SecondaryCJSCode;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static java.math.BigInteger.valueOf;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendarFromLocalDate;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.*;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PleaValue.GUILTY;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PleaValue.MCA_GUILTY;

@ExtendWith(MockitoExtension.class)
public class CourtOffenceTest {

    private static final String MOT_REASON_CODE = "10";
    private static final String MOT_REASON_CODE_WITH_ZERO_PREFIX = "01";
    private static final int NO_PLEA_TAKEN_VALUE = 3;

    private static final LocalDate LOCAL_DATE = LocalDate.of(2024, 01, 17);

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @InjectMocks
    private CourtOffence courtOffence;

    @Test
    public void testBuildCourtOffenceStructure() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, MCA_GUILTY.name());
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        assertThat(courtOffenceStructure.getConvictingCourt(), is(valueOf(psaCode)));
        assertThat(courtOffenceStructure.getConvictionDate(), is(getXmlGregorianCalendar(offenceDetails.getConvictionDate())));
        assertThat(courtOffenceStructure.getFinalDisposalIndicator(), is(offenceDetails.getFinalDisposal()));
        assertThat(courtOffenceStructure.getInitiatedDate(), is(getXmlGregorianCalendar(offenceDetails.getStartDate())));
        assertThat(courtOffenceStructure.getFinding(), is(offenceDetails.getFinding()));
        assertThat(courtOffenceStructure.getModeOfTrial().toString(), is(offenceDetails.getModeOfTrial()));
        assertThat(courtOffenceStructure.getPlea(), is(1));
        assertBaseOffenceDetails(offenceDetails, courtOffenceStructure);
    }

    @Test
    public void shouldReturnExpectedPlea() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        final Integer expectedPlea = 2;
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(expectedPlea));
        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, MCA_GUILTY.name());
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getPlea(), is(expectedPlea));
    }

    @Test
    public void shouldReturnConvictingCourtWhenThereIsConvictionDate() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        OffenceDetails offenceDetails = buildOffenceDetailsWithConvictionDate(LOCAL_DATE);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();
        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getConvictingCourt(), is(valueOf(psaCode)));
    }

    @Test
    public void shouldNotReturnConvictingCourtWhenThereIsNoConvictionDate() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        OffenceDetails offenceDetails = buildOffenceDetailsWithConvictionDate(null);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();
        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getConvictingCourt(), is(nullValue()));
    }

    @Test
    public void shouldSetPleaToNoPleaTakenWhenPleaStatusCodeIsZero() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        final Integer notknownStatusValue = 0;
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(notknownStatusValue));
        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, MCA_GUILTY.name());
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getPlea(), is(NO_PLEA_TAKEN_VALUE));
    }

    @Test
    public void shouldSetPleaToNoPleaTakenWhenPleaDoesNotExistsInOffenceDetails() {

        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        verify(referenceDataQueryService, never()).retrievePleaStatusCode(any());
        assertThat(courtOffenceStructure.getPlea(), is(NO_PLEA_TAKEN_VALUE));
    }

    @Test
    public void shouldSetPleaToNoPleaTakenWhenPleaValueIsNullInOffenceDetails() {

        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, null);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        verify(referenceDataQueryService, never()).retrievePleaStatusCode(any());
        assertThat(courtOffenceStructure.getPlea(), is(NO_PLEA_TAKEN_VALUE));
    }

    @Test
    public void shouldReturnExpectedPleaForIndicatorPlea() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        final Integer expectedPlea = 2;
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(expectedPlea));
        OffenceDetails offenceDetails = buildOffenceDetailsWithIndicatedPlea(MOT_REASON_CODE, IndicatedPleaValue.INDICATED_GUILTY);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getPlea(), is(expectedPlea));
    }

    @Test
    public void shouldReturnExpectedPleaForIndicatorNotGuiltyPlea() {
        final Integer expectedPlea = 3;
        OffenceDetails offenceDetails = buildOffenceDetailsWithIndicatedPlea(MOT_REASON_CODE, IndicatedPleaValue.INDICATED_NOT_GUILTY);
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertThat(courtOffenceStructure.getPlea(), is(expectedPlea));
    }

    @Test
    public void testMotReasonCodeWithPrefixZero() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE_WITH_ZERO_PREFIX, GUILTY.name());
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertMotReasonCode(offenceDetails, courtOffenceStructure);

    }

    @Test
    public void testTwoDigitMotReasonCode() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, GUILTY.name());
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertMotReasonCode(offenceDetails, courtOffenceStructure);

    }

    @Test
    public void testBuildCourtOffenceStructureShouldExcludeJudicialResultWhenCJSCodeIsNull() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, GUILTY.name());
        offenceDetails.getJudicialResults().add(judicialResult().withCjsCode(null).build());
        assertThat(offenceDetails.getJudicialResults().size(), is(2));

        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        assertThat(courtOffenceStructure.getResult().size(), is(2));
        assertThat(courtOffenceStructure.getResult().get(0).getResultCode(), is(12345));
        assertThat(courtOffenceStructure.getResult().get(1).getResultCode(), is(nullValue()));
    }

    @Test
    public void shouldBuildCourtOffenceStructureWithJudicialResultWhenCJSCodeIsEmpty() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        OffenceDetails offenceDetails = buildOffenceDetailsWithMotReasonCode(MOT_REASON_CODE, GUILTY.name());
        offenceDetails.getJudicialResults().add(judicialResult().withCjsCode("").build());
        assertThat(offenceDetails.getJudicialResults().size(), is(2));

        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        assertThat(courtOffenceStructure.getResult().size(), is(2));
        assertThat(courtOffenceStructure.getResult().get(0).getResultCode(), is(12345));
        assertThat(courtOffenceStructure.getResult().get(1).getResultCode(), is(nullValue()));
    }

    @Test
    public void shouldBuildCourtOffenceStructureWithResultsFromSecondaryCjsCodesWhenAvailable() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        final OffenceDetails offenceDetails = buildOffenceDetailsWithSecondaryCjsCodes();
        Integer psaCode = createPublicPoliceResultGenerated().getCourtCentreWithLJA().getPsaCode();
        final HashMap<String, Object> context = new HashMap<>();

        final CourtOffenceStructure courtOffenceStructure = courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));

        assertThat(courtOffenceStructure.getResult(), hasSize(3));
        assertThat(courtOffenceStructure.getResult().get(0).getResultCode(), is(12345));
        assertThat(courtOffenceStructure.getResult().get(0).getResultText(), is(RESULT_TEXT_2500));// result text truncated to 2500 characters

        final List<SecondaryCJSCode> expectedSecondaryCJSCodes = offenceDetails.getJudicialResults().get(0).getSecondaryCJSCodes();
        assertThat(courtOffenceStructure.getResult().get(1).getResultCode(), is(Integer.valueOf(expectedSecondaryCJSCodes.get(0).getCjsCode())));
        assertThat(courtOffenceStructure.getResult().get(1).getResultText(), is(RESULT_TEXT_2500)); // result text truncated to 2500 characters
        assertThat(courtOffenceStructure.getResult().get(2).getResultCode(), is(Integer.valueOf(expectedSecondaryCJSCodes.get(1).getCjsCode())));
        assertThat(courtOffenceStructure.getResult().get(2).getResultText(), is(expectedSecondaryCJSCodes.get(1).getText()));
    }

    private void assertBaseOffenceDetails(final OffenceDetails offenceDetails, final CourtOffenceStructure courtOffenceStructure) {
        if (null != courtOffenceStructure.getBaseOffenceDetails()) {
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceCode(), is(offenceDetails.getOffenceCode()));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceWording(), is(offenceDetails.getWording()));
            assertOffenceTiming(offenceDetails, courtOffenceStructure);
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getArrestDate(), is(getXmlGregorianCalendar(offenceDetails.getArrestDate())));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getChargeDate(), is(getXmlGregorianCalendar(offenceDetails.getChargeDate())));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceSequenceNumber(), is(offenceDetails.getOffenceSequenceNumber()));
            assertAlcoholRelatedOffence(offenceDetails, courtOffenceStructure);
            assertVehicleRelatedOffence(offenceDetails, courtOffenceStructure);
        }
    }

    private void assertVehicleRelatedOffence(final OffenceDetails offenceDetails, final CourtOffenceStructure courtOffenceStructure) {
        if (null != courtOffenceStructure.getBaseOffenceDetails().getVehicleRelatedOffence()) {
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getVehicleRelatedOffence().getVehicleCode(), is("L"));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getVehicleRelatedOffence().getVehicleRegistrationMark(), is(offenceDetails.getOffenceFacts().getVehicleRegistration()));
        }
    }

    private void assertAlcoholRelatedOffence(final OffenceDetails offenceDetails, final CourtOffenceStructure courtOffenceStructure) {
        if (null != courtOffenceStructure.getBaseOffenceDetails().getAlcoholRelatedOffence()) {
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getAlcoholRelatedOffence().getAlcoholLevelMethod(), is(offenceDetails.getOffenceFacts().getAlcoholReadingMethodCode()));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getAlcoholRelatedOffence().getAlcoholLevelAmount(), is(offenceDetails.getOffenceFacts().getAlcoholReadingAmount()));
        }
    }

    private void assertOffenceTiming(final OffenceDetails offenceDetails, final CourtOffenceStructure courtOffenceStructure) {
        if (null != courtOffenceStructure.getBaseOffenceDetails().getOffenceTiming()) {
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceTiming().getOffenceStart().getOffenceDateStartDate(), is(getXmlGregorianCalendar(offenceDetails.getStartDate())));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceTiming().getOffenceEnd().getOffenceEndDate(), is(getXmlGregorianCalendar(offenceDetails.getEndDate())));
            assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceTiming().getOffenceDateCode(), is(valueOf(offenceDetails.getOffenceDateCode())));
        }
    }

    private void assertMotReasonCode(final OffenceDetails offenceDetails, final CourtOffenceStructure courtOffenceStructure) {
        if (null != offenceDetails.getAllocationDecision() && isNotBlank(offenceDetails.getAllocationDecision().getMotReasonCode())) {
            assertThat(courtOffenceStructure.getModeOfTrial(), is((Integer.valueOf(offenceDetails.getAllocationDecision().getMotReasonCode()))));
        }
    }
}
