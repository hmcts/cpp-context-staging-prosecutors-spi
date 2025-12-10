package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.lang.Integer.valueOf;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.HEARING_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.LOCAL_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.RESULT_TEXT_2500;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildJudicialResult;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import org.junit.jupiter.api.Test;
import uk.gov.dca.xmlschemas.libra.BaseHearingStructure;
import uk.gov.dca.xmlschemas.libra.BaseNextHearingStructure;
import uk.gov.dca.xmlschemas.libra.CourtOutcomeStructure;
import uk.gov.dca.xmlschemas.libra.CourtResultStructure;
import uk.gov.justice.core.courts.JudicialResult;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;


public class CourtResultTest {

    private static final String NEXT_HEARING_REASON = "Unexpected circumstances meant that the case could not be heard For a live link ";
    private static final String SECONDARY_CJS_TEXT = "NonNumericSecondaryCjsCode";
    private final XMLGregorianCalendar dateOfHearing = getXmlGregorianCalendar(HEARING_DATE);

    @Test
    public void testBuildCourtResultStructuresWithOnlyTotalPenaltyPoints() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, true, false, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome(), notNullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration(), notNullValue());
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints().toString(), is(judicialResult.getJudicialResultPrompts().get(0).getTotalPenaltyPoints().toString()));
    }

    @Test
    public void testBuildCourtResultStructuresWithOnlyIsFinancialImposition() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, true, false, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration(), nullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling().intValue(), is(Integer.valueOf(judicialResult.getJudicialResultPrompts().get(0).getValue().substring(1))));
    }

    @Test
    public void testBuildCourtResultStructuresWithOnlyDurationElement() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, false, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints(), nullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationUnit(), is(judicialResult.getDurationElement().getPrimaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationValue(), is(judicialResult.getDurationElement().getPrimaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
    }

    @Test
    public void testBuildCourtResultStructuresWithOnlyDurationIncludingStartDateElement() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, false, true, true, false, 1000);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints(), nullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(2).getValue().toString(), is(String.valueOf(HEARING_DATE.plusDays(judicialResult.getDurationElement().getPrimaryDurationValue()))));
    }

    @Test
    public void testBuildCourtResultStructuresWithOnlyDurationIncludingEndDateElement() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, false, true, false, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints(), nullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationUnit(), is(judicialResult.getDurationElement().getPrimaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationValue(), is(judicialResult.getDurationElement().getPrimaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(0).getValue().toString(), is("2005-09-19"));
    }

    @Test
    public void testBuildCourtResultStructuresWithOnlyDurationIncludingStartAndEndDateElement() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, false, true, true, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints(), nullValue());
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling(), nullValue());
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationUnit(), is(judicialResult.getDurationElement().getPrimaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationValue(), is(judicialResult.getDurationElement().getPrimaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(0).getValue().toString(), is("2019-09-19"));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(1).getValue().toString(), is("2005-09-19"));
    }

    @Test
    public void testBuildCourtResultStructuresWithAllTheOutcomeFields() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, true, true, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getPenaltyPoints().toString(), is(judicialResult.getJudicialResultPrompts().get(0).getTotalPenaltyPoints().toString()));
        assertThat(courtResultStructure.getOutcome().getResultAmountSterling().intValue(), is(Integer.valueOf(judicialResult.getJudicialResultPrompts().get(0).getValue().substring(1))));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationUnit(), is(judicialResult.getDurationElement().getPrimaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationValue(), is(judicialResult.getDurationElement().getPrimaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
    }

    @Test
    public void testBuildCourtResultStructuresWithNoOutcomeFields() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, true, false, false, false, false, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));

        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome(), nullValue());
    }

    @Test
    public void testBuildCourtResultStructures() {
        final JudicialResult judicialResult = buildJudicialResult(true, true);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getResultText(), is(RESULT_TEXT_2500));
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getCjsCode())));
        assertThat(courtResultStructure.getResultCodeQualifier(), hasSize(4));
        assertThat(courtResultStructure.getResultCodeQualifier().get(0), is("qualifier"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(1), is("qual1"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(2), is("qual2"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(3), is("qual3"));

        final BaseNextHearingStructure nextHearing = courtResultStructure.getNextHearing();
        assertThat(nextHearing.getBailStatusOffence(), is("C"));
        assertThat(nextHearing.getNextHearingReason().length(), is(80));
        assertThat(nextHearing.getNextHearingReason(), is(NEXT_HEARING_REASON));
        final ZonedDateTime listedStartDateTime = judicialResult.getNextHearing().getListedStartDateTime();
        final BaseHearingStructure nextHearingDetails = nextHearing.getNextHearingDetails();
        assertThat(nextHearingDetails.getDateOfHearing(), is(getXmlGregorianCalendar(listedStartDateTime)));
        assertThat(nextHearingDetails.getTimeOfHearing(), is(getXmlGregorianCalendar(listedStartDateTime)));
        assertThat(nextHearingDetails.getCourtHearingLocation(), is("COURT12345"));

        final CourtOutcomeStructure outcome = courtResultStructure.getOutcome();
        assertThat(outcome.getPenaltyPoints(), is(12));
        assertThat(outcome.getResultAmountSterling(), is(new BigDecimal(10).setScale(2, BigDecimal.ROUND_DOWN)));

    }

    @Test
    public void shouldBuildCourtResultStructuresWithoutNonNumericSecondaryCjsCodes() {
        final JudicialResult judicialResult = buildJudicialResult(false, true, true, true);
        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(3));
        assertThat(courtResultStructures.stream().anyMatch(courtResultStructure -> courtResultStructure.getResultText().equals(SECONDARY_CJS_TEXT)), is(false));
    }

    @Test
    public void testBuildCourtResultStructuresWithNullListedStartDateTime() {
        final JudicialResult judicialResult = buildJudicialResult(true, false);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getResultText(), is(RESULT_TEXT_2500));
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getCjsCode())));
        assertThat(courtResultStructure.getResultCodeQualifier(), hasSize(4));
        assertThat(courtResultStructure.getResultCodeQualifier().get(0), is("qualifier"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(1), is("qual1"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(2), is("qual2"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(3), is("qual3"));

        assertThat(courtResultStructure.getNextHearing(), nullValue());

        final CourtOutcomeStructure outcome = courtResultStructure.getOutcome();
        assertThat(outcome.getPenaltyPoints(), is(12));
        assertThat(outcome.getResultAmountSterling(), is(new BigDecimal(10).setScale(2, BigDecimal.ROUND_DOWN)));

    }

    @Test
    public void testBuildCourtResultStructuresWithNullNextHearingLocation() {
        final JudicialResult judicialResult = buildJudicialResult(true, true, false, false);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getResultText(), is(RESULT_TEXT_2500));
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getCjsCode())));
        assertThat(courtResultStructure.getResultCodeQualifier(), hasSize(4));
        assertThat(courtResultStructure.getResultCodeQualifier().get(0), is("qualifier"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(1), is("qual1"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(2), is("qual2"));
        assertThat(courtResultStructure.getResultCodeQualifier().get(3), is("qual3"));

        assertThat(courtResultStructure.getNextHearing(), nullValue());

        final CourtOutcomeStructure outcome = courtResultStructure.getOutcome();
        assertThat(outcome.getPenaltyPoints(), is(12));
        assertThat(outcome.getResultAmountSterling(), is(new BigDecimal(10).setScale(2, BigDecimal.ROUND_DOWN)));

    }

    @Test
    public void testBuildCourtResultStructuresWithoutNextHearing() {
        final JudicialResult judicialResult = buildJudicialResult(false, true);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        final BaseNextHearingStructure nextHearing = courtResultStructure.getNextHearing();
        assertThat(nextHearing, nullValue());
    }

    @Test
    public void testBuildCourtResultStructuresWhenJudicialResultPromptDurationElementIsPresent() {
        final JudicialResult judicialResult = buildJudicialResult(false, true, true, false, true, true, true, true, true, 1);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationUnit(), is(judicialResult.getDurationElement().getPrimaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationValue(), is(judicialResult.getDurationElement().getPrimaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationUnit(), is(judicialResult.getDurationElement().getSecondaryDurationUnit()));
        assertThat(courtResultStructure.getOutcome().getDuration().getSecondaryDurationValue(), is(judicialResult.getDurationElement().getSecondaryDurationValue()));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(0).getValue().toString(), is("2019-09-19"));
        assertThat(courtResultStructure.getOutcome().getDuration().getDurationStartAndEnd().get(1).getValue().toString(), is("2005-09-19"));
    }

    @Test
    public void testBuildCourtResultStructuresWhenJudicialResultPromptDurationElementNotPresent() {
        final JudicialResult judicialResult = judicialResult().withJudicialResultPrompts(singletonList(judicialResultPrompt()
                .withCourtExtract("Y")
                .withLabel("label")
                .withDurationSequence(1)
                .withPromptReference("")
                .withPromptSequence(BigDecimal.ONE)
                .withUsergroups(Arrays.asList("GROUP1", "GROUP2"))
                .withValue("10")
                .withWelshLabel("WS")
                .withTotalPenaltyPoints(new BigDecimal(12))
                .withIsFinancialImposition(true)
                .build())).withDurationElement(null).build();

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(1));
        final CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertNull(courtResultStructure.getOutcome().getDuration());
    }

    @Test
    public void shouldBuildCourtResultStructuresWithResultsFromSecondaryCjsCodes() {
        final JudicialResult judicialResult = buildJudicialResult(false, false, false, true);

        final List<CourtResultStructure> courtResultStructures = new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing);

        assertThat(courtResultStructures, hasSize(3));
        CourtResultStructure courtResultStructure = courtResultStructures.get(0);
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getCjsCode())));
        assertThat(courtResultStructure.getResultText(), is(RESULT_TEXT_2500));

        courtResultStructure = courtResultStructures.get(1);
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getSecondaryCJSCodes().get(0).getCjsCode())));
        assertThat(courtResultStructure.getResultText(), is(RESULT_TEXT_2500));

        courtResultStructure = courtResultStructures.get(2);
        assertThat(courtResultStructure.getResultCode(), is(valueOf(judicialResult.getSecondaryCJSCodes().get(1).getCjsCode())));
        assertThat(courtResultStructure.getResultText(), is(judicialResult.getSecondaryCJSCodes().get(1).getText()));
    }
}
