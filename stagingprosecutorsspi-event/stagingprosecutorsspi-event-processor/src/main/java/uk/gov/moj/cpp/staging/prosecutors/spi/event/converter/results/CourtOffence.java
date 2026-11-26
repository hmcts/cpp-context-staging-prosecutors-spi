package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.BooleanUtils.isNotTrue;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendarFromLocalDate;

import uk.gov.dca.xmlschemas.libra.BaseOffenceDetailStructure;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.dca.xmlschemas.libra.CourtResultStructure;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

public class CourtOffence {

    private static final int UNSUPPORTED_PLEA_VALUE = 0;
    private static final int NO_PLEA_TAKEN_VALUE = 3;
    private static final String LARGE_GOODS_OR_PASSENGER_CARRYING_VEHICLE_CODE = "L";
    private static final String OTHER_VEHICLE_CODE = "O";


    @Inject
    private ReferenceDataQueryService referenceDataQueryService;

    public CourtOffenceStructure buildCourtOffenceStructure(final OffenceDetails offenceDetails, final Integer psaCode, final Map<String, Object> offenceLocationContext, final XMLGregorianCalendar dateOfHearing) {
        final CourtOffenceStructure courtOffenceStructure = new CourtOffenceStructure();
        setConvictingCourtBasedOnConvictionDate(offenceDetails, psaCode, courtOffenceStructure);

        courtOffenceStructure.setConvictionDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getConvictionDate()));
        courtOffenceStructure.setFinalDisposalIndicator(offenceDetails.getFinalDisposal());

        if (nonNull(offenceDetails.getAllocationDecision()) && isNotEmpty(offenceDetails.getAllocationDecision().getMotReasonCode())) {
            courtOffenceStructure.setModeOfTrial(Integer.valueOf(offenceDetails.getAllocationDecision().getMotReasonCode()));
        }

        courtOffenceStructure.setBaseOffenceDetails(buildBaseOffenceDetailsStructure(offenceDetails, offenceLocationContext));
        courtOffenceStructure.setInitiatedDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getStartDate()));

        setPleaValueToCourtOffenceStructure(offenceDetails, courtOffenceStructure);

        if (null != offenceDetails.getJudicialResults()) {
            courtOffenceStructure.getResult().addAll(buildCourtResultStructureList(offenceDetails.getJudicialResults(), dateOfHearing));
        }
        courtOffenceStructure.setFinding(offenceDetails.getFinding());

        return courtOffenceStructure;
    }

    private void setPleaValueToCourtOffenceStructure(OffenceDetails offenceDetails, CourtOffenceStructure courtOffenceStructure) {
        if ( (nonNull(offenceDetails.getPlea()) && isNotEmpty(offenceDetails.getPlea().getPleaValue()) ) ||
                (nonNull(offenceDetails.getIndicatedPlea()) && nonNull(offenceDetails.getIndicatedPlea().getIndicatedPleaValue()) &&
                        IndicatedPleaValue.INDICATED_GUILTY.equals(offenceDetails.getIndicatedPlea().getIndicatedPleaValue()) )) {
            final int pleaValue = getPlea(ofNullable(offenceDetails.getPlea()).map(Plea::getPleaValue).orElseGet(() -> offenceDetails.getIndicatedPlea().getIndicatedPleaValue().name()));
            if (UNSUPPORTED_PLEA_VALUE != pleaValue) {
                courtOffenceStructure.setPlea(pleaValue);
            } else {
                courtOffenceStructure.setPlea(NO_PLEA_TAKEN_VALUE);
            }
        } else {
            courtOffenceStructure.setPlea(NO_PLEA_TAKEN_VALUE);
        }
    }

    private static void setConvictingCourtBasedOnConvictionDate(OffenceDetails offenceDetails, Integer psaCode, CourtOffenceStructure courtOffenceStructure) {
        if(nonNull(offenceDetails.getConvictionDate())) {
            courtOffenceStructure.setConvictingCourt(valueOf(psaCode));
        }
    }

    private int getPlea(String pleaValue) {
        return referenceDataQueryService.retrievePleaStatusCode(pleaValue)
                .orElseThrow(() -> new IllegalArgumentException(String.format("PleaValue (%s) is not found in reference data!", pleaValue)));
    }

    private List<CourtResultStructure> buildCourtResultStructureList(final List<JudicialResult> judicialResults, final XMLGregorianCalendar dateOfHearing) {
        final List<CourtResultStructure> courtResultStructureList = new ArrayList<>();
        judicialResults.stream()
                .filter(judicialResult -> isNotTrue(judicialResult.getPublishedForNows()))
                .forEach(judicialResult -> courtResultStructureList.addAll(new CourtResult().buildCourtResultStructures(judicialResult, dateOfHearing)));
        return courtResultStructureList;
    }

    private BaseOffenceDetailStructure buildBaseOffenceDetailsStructure(final OffenceDetails offenceDetails, final Map<String, Object> offenceLocationContext) {
        final BaseOffenceDetailStructure baseOffenceDetailStructure = new BaseOffenceDetailStructure();
        baseOffenceDetailStructure.setArrestDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getArrestDate()));
        baseOffenceDetailStructure.setChargeDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getChargeDate()));
        baseOffenceDetailStructure.setOffenceCode(offenceDetails.getOffenceCode());
        baseOffenceDetailStructure.setOffenceSequenceNumber(offenceDetails.getOffenceSequenceNumber());
        baseOffenceDetailStructure.setOffenceWording(offenceDetails.getWording());
        final String offenceId = offenceDetails.getId().toString();
        if (offenceLocationContext.containsKey(offenceId)) {
            baseOffenceDetailStructure.setLocationOfOffence(String.valueOf(offenceLocationContext.get(offenceId)));
        }
        buildOffenceTiming(offenceDetails, baseOffenceDetailStructure);

        if (null != offenceDetails.getOffenceFacts()) {
            final OffenceFacts offenceFacts = offenceDetails.getOffenceFacts();

            if (null != offenceFacts.getAlcoholReadingAmount()) {
                final BaseOffenceDetailStructure.AlcoholRelatedOffence alcoholRelatedOffence = new BaseOffenceDetailStructure.AlcoholRelatedOffence();
                alcoholRelatedOffence.setAlcoholLevelAmount(offenceFacts.getAlcoholReadingAmount());
                alcoholRelatedOffence.setAlcoholLevelMethod(offenceFacts.getAlcoholReadingMethodCode());
                baseOffenceDetailStructure.setAlcoholRelatedOffence(alcoholRelatedOffence);
            }

            if (null != offenceFacts.getVehicleCode()) {
                final BaseOffenceDetailStructure.VehicleRelatedOffence vehicleRelatedOffence = new BaseOffenceDetailStructure.VehicleRelatedOffence();
                vehicleRelatedOffence.setVehicleCode(getVehicleCode(offenceFacts.getVehicleCode()));
                vehicleRelatedOffence.setVehicleRegistrationMark(offenceFacts.getVehicleRegistration());
                baseOffenceDetailStructure.setVehicleRelatedOffence(vehicleRelatedOffence);
            }
        }


        return baseOffenceDetailStructure;
    }

    private String getVehicleCode(VehicleCode vehicleCode) {
        switch (vehicleCode) {
            case LARGE_GOODS_VEHICLE:
                return LARGE_GOODS_OR_PASSENGER_CARRYING_VEHICLE_CODE;
            case OTHER:
                return OTHER_VEHICLE_CODE;
            case PASSENGER_CARRYING_VEHICLE:
                return LARGE_GOODS_OR_PASSENGER_CARRYING_VEHICLE_CODE;

        }
        return null;
    }

    private void buildOffenceTiming(final OffenceDetails offenceDetails, final BaseOffenceDetailStructure baseOffenceDetailStructure) {
        final BaseOffenceDetailStructure.OffenceTiming offenceTiming = new BaseOffenceDetailStructure.OffenceTiming();
        offenceTiming.setOffenceDateCode(valueOf(offenceDetails.getOffenceDateCode()));

        final BaseOffenceDetailStructure.OffenceTiming.OffenceStart startDate = new BaseOffenceDetailStructure.OffenceTiming.OffenceStart();
        startDate.setOffenceDateStartDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getStartDate()));
        offenceTiming.setOffenceStart(startDate);

        final BaseOffenceDetailStructure.OffenceTiming.OffenceEnd endDate = new BaseOffenceDetailStructure.OffenceTiming.OffenceEnd();
        if (null != offenceDetails.getEndDate()) {
            endDate.setOffenceEndDate(getXmlGregorianCalendarFromLocalDate(offenceDetails.getEndDate()));
            offenceTiming.setOffenceEnd(endDate);
        }
        baseOffenceDetailStructure.setOffenceTiming(offenceTiming);
    }

}
