package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.valueOf;
import static java.math.BigDecimal.ROUND_DOWN;
import static java.time.LocalDate.parse;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.left;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendar;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendarFromLocalDate;

import uk.gov.dca.xmlschemas.libra.BaseHearingStructure;
import uk.gov.dca.xmlschemas.libra.BaseNextHearingStructure;
import uk.gov.dca.xmlschemas.libra.CourtOutcomeStructure;
import uk.gov.dca.xmlschemas.libra.CourtResultStructure;
import uk.gov.dca.xmlschemas.libra.ObjectFactory;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.justice.core.courts.NextHearing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;

public class CourtResult {

    private static final int RESULT_TEXT_MAX_LENGTH = 2500;
    private static final int DURATION_LIMIT = 999;
    private static final int DECIMAL_PLACES = 2;
    private static final String COMMA = ",";
    private static final int NEXT_HEARING_MAX_LENGTH = 80;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CourtResult.class);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy");
    private static final String PROMPT_TYPE_BOOLEAN = "BOOLEAN";
    private static final String DELIMETER = ",";
    private static final String FALSE = "FALSE";
    private static final String NO_VALUE = "NO";

    @SuppressWarnings("PMD.NullAssignment")
    public List<CourtResultStructure> buildCourtResultStructures(final JudicialResult judicialResult, final XMLGregorianCalendar dateOfHearing) {
        final List<CourtResultStructure> courtResults = newArrayList();
        final CourtResultStructure courtResultStructure = new CourtResultStructure();
        courtResultStructure.setResultCode(isNotBlank(judicialResult.getCjsCode()) ? valueOf(judicialResult.getCjsCode()) : null);
        courtResultStructure.setResultText(left(judicialResult.getResultText(), RESULT_TEXT_MAX_LENGTH));

        populateMarker(judicialResult, courtResultStructure);
        courtResultStructure.setNextHearing(buildNextHearingStructure(judicialResult));
        courtResultStructure.setOutcome(null != judicialResult.getJudicialResultPrompts() ? buildOutcomeStructure(judicialResult, dateOfHearing) : null);

        courtResults.add(courtResultStructure);
        if (isNotEmpty(judicialResult.getSecondaryCJSCodes())) {
            final List<CourtResultStructure> secondaryResults = judicialResult.getSecondaryCJSCodes()
                    .stream()
                    .filter(secondaryCJSCode -> isNumeric(secondaryCJSCode.getCjsCode()))
                    .map(secondaryCJSCode -> {
                        final CourtResultStructure secondaryResult = new CourtResultStructure();
                        final String cjsCode = secondaryCJSCode.getCjsCode();

                        secondaryResult.setResultCode(valueOf(cjsCode));
                        secondaryResult.setResultText(left(secondaryCJSCode.getText(), RESULT_TEXT_MAX_LENGTH));

                        return secondaryResult;
                    }).collect(toList());
            courtResults.addAll(secondaryResults);
        }
        return courtResults;
    }

    private CourtOutcomeStructure buildOutcomeStructure(final JudicialResult judicialResult, final XMLGregorianCalendar dateOfHearing) {
        final List<JudicialResultPrompt> judicialResultPrompts = judicialResult.getJudicialResultPrompts();
        final JudicialResultPromptDurationElement resultDurationElement = judicialResult.getDurationElement();

        CourtOutcomeStructure courtOutcomeStructure = null;

        final Optional<BigDecimal> penaltyPointOptional = judicialResultPrompts.stream().filter(p -> null != p.getTotalPenaltyPoints()).findFirst().map(JudicialResultPrompt::getTotalPenaltyPoints);
        if (penaltyPointOptional.isPresent()) {
            courtOutcomeStructure = new CourtOutcomeStructure();
            courtOutcomeStructure.setPenaltyPoints(penaltyPointOptional.get().intValue());
        }

        final Optional<String> amountSterlingOptional = judicialResultPrompts.stream().filter(p -> null != p.getIsFinancialImposition() && p.getIsFinancialImposition()).findFirst().map(JudicialResultPrompt::getValue);
        if (amountSterlingOptional.isPresent()) {
            if (courtOutcomeStructure == null) {
                courtOutcomeStructure = new CourtOutcomeStructure();
            }

            courtOutcomeStructure.setResultAmountSterling(new BigDecimal(stripAmountSterling(amountSterlingOptional.get())).setScale(DECIMAL_PLACES, ROUND_DOWN));
        }


        return setDurationInCourtOutcomeStructure(resultDurationElement, courtOutcomeStructure, dateOfHearing);
    }

    private String stripAmountSterling(final String amountSterling) {
        return amountSterling.startsWith("£") ? amountSterling.substring(1) : amountSterling;
    }

    private CourtOutcomeStructure setDurationInCourtOutcomeStructure(final JudicialResultPromptDurationElement resultDurationElement, CourtOutcomeStructure courtOutcomeStructure, final XMLGregorianCalendar dateOfHearing) {

        if (resultDurationElement != null) {
            final CourtOutcomeStructure.Duration duration = new CourtOutcomeStructure.Duration();
            if (nonNull(resultDurationElement.getPrimaryDurationUnit()) && resultDurationElement.getPrimaryDurationValue() <= DURATION_LIMIT) {
                duration.setDurationUnit(resultDurationElement.getPrimaryDurationUnit());
                duration.setDurationValue(resultDurationElement.getPrimaryDurationValue());
            }
            if (nonNull(resultDurationElement.getSecondaryDurationUnit()) && resultDurationElement.getSecondaryDurationValue() <= DURATION_LIMIT) {
                duration.setSecondaryDurationUnit(resultDurationElement.getSecondaryDurationUnit());
                duration.setSecondaryDurationValue(resultDurationElement.getSecondaryDurationValue());
            }
            duration.getDurationStartAndEnd().addAll(populateStartAndEndDate(resultDurationElement, dateOfHearing));
            if (courtOutcomeStructure == null) {
                courtOutcomeStructure = new CourtOutcomeStructure();
            }
            courtOutcomeStructure.setDuration(duration);
        }
        return courtOutcomeStructure;
    }

    private List<JAXBElement<XMLGregorianCalendar>> populateStartAndEndDate(final JudicialResultPromptDurationElement resultDurationElement, final XMLGregorianCalendar dateOfHearing) {
        final List<JAXBElement<XMLGregorianCalendar>> startAndEndDateElement = new ArrayList<>();
        final LocalDate hearingDate = dateOfHearing.toGregorianCalendar().toZonedDateTime().toLocalDate();
        if (nonNull(resultDurationElement.getDurationStartDate())) {
            LOGGER.info("Duration start date is present and including in spiout message");
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationStartDate(getXmlGregorianCalendarFromLocalDate(parse(resultDurationElement.getDurationStartDate(), dateFormatter))));
        }
        if (nonNull(resultDurationElement.getPrimaryDurationValue()) && resultDurationElement.getPrimaryDurationValue() > DURATION_LIMIT) {
            LOGGER.info("Primary duration value is present and including in spiout message");
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationStartDate(getXmlGregorianCalendarFromLocalDate(hearingDate)));
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationEndDate(getXmlGregorianCalendarFromLocalDate(getEndDate(hearingDate, resultDurationElement.getPrimaryDurationValue()))));
        } else if (nonNull(resultDurationElement.getSecondaryDurationValue()) && resultDurationElement.getSecondaryDurationValue() > DURATION_LIMIT) {
            LOGGER.info("Secondary duration value is present and including in spiout message");
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationStartDate(getXmlGregorianCalendarFromLocalDate(hearingDate)));
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationEndDate(getXmlGregorianCalendarFromLocalDate(getEndDate(hearingDate, resultDurationElement.getSecondaryDurationValue()))));
        } else if (nonNull(resultDurationElement.getDurationEndDate())) {
            LOGGER.info("Duration end date is present and including in spiout message");
            startAndEndDateElement.add(new ObjectFactory().createCourtOutcomeStructureDurationDurationEndDate(getXmlGregorianCalendarFromLocalDate(parse(resultDurationElement.getDurationEndDate(), dateFormatter))));
        }
        return startAndEndDateElement;
    }

    private LocalDate getEndDate(LocalDate durationStartDate, Integer durationValue) {
        return durationStartDate.plusDays(durationValue);
    }

    private void populateMarker(final JudicialResult judicialResult, final CourtResultStructure courtResultStructure) {
        if (null != judicialResult.getQualifier()) {
            String judicialResultQualifier = judicialResult.getQualifier();
            if (nonNull(judicialResult.getJudicialResultPrompts())) {
                final List<String> judicialResultQualifiers = judicialResult.getJudicialResultPrompts().stream()
                        .filter(Objects::nonNull)
                        .filter(jrp -> nonNull(jrp.getQualifier()) && PROMPT_TYPE_BOOLEAN.equals(jrp.getType())
                                && (FALSE.equalsIgnoreCase(jrp.getValue())
                                || NO_VALUE.equalsIgnoreCase(jrp.getValue())))
                        .map(JudicialResultPrompt::getQualifier).collect(Collectors.toList());
                if (!judicialResultQualifiers.isEmpty()) {
                    judicialResultQualifier = Pattern.compile(DELIMETER).splitAsStream(judicialResultQualifier)
                            .filter(s -> !judicialResultQualifiers.contains(s))
                            .collect(Collectors.joining(DELIMETER));
                }
            }
            if (judicialResultQualifier.length() > 0) {
                final String[] splitJudicialQualifier = judicialResultQualifier.split(COMMA);
                stream(splitJudicialQualifier).limit(4).forEach(q -> courtResultStructure.getResultCodeQualifier().add(q));
            }
        }
    }


    private BaseNextHearingStructure buildNextHearingStructure(final JudicialResult judicialResult) {

        if (isNull(judicialResult.getNextHearing()) || isNull(judicialResult.getNextHearing().getCourtCentre()) || isNull(judicialResult.getNextHearing().getListedStartDateTime())) {
            return null;
        }

        final BaseNextHearingStructure baseNextHearingStructure = new BaseNextHearingStructure();

        if (isNotEmpty(judicialResult.getNextHearing().getAdjournmentReason())) {
            baseNextHearingStructure.setNextHearingReason(left(judicialResult.getNextHearing().getAdjournmentReason(), NEXT_HEARING_MAX_LENGTH));
        }

        baseNextHearingStructure.setNextHearingDetails(buildBaseHearingStructure(judicialResult.getNextHearing()));
        baseNextHearingStructure.setBailStatusOffence(judicialResult.getPostHearingCustodyStatus());
        return baseNextHearingStructure;

    }

    private BaseHearingStructure buildBaseHearingStructure(final NextHearing nextHearing) {

        final BaseHearingStructure baseHearingStructure = new BaseHearingStructure();
        baseHearingStructure.setCourtHearingLocation(nextHearing.getCourtCentre().getCourtHearingLocation());

        final XMLGregorianCalendar listedStartDateTime = getXmlGregorianCalendar(nextHearing.getListedStartDateTime());
        baseHearingStructure.setDateOfHearing(listedStartDateTime);
        baseHearingStructure.setTimeOfHearing(listedStartDateTime);

        return baseHearingStructure;
    }
}
