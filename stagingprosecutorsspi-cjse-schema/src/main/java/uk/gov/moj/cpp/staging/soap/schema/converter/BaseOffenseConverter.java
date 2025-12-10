package uk.gov.moj.cpp.staging.soap.schema.converter;


import static java.util.Objects.isNull;

import uk.gov.dca.xmlschemas.libra.BaseOffenceDetailStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BaseOffense;

import java.time.LocalDate;

@SuppressWarnings("squid:S1067")
public class BaseOffenseConverter implements Converter<BaseOffenceDetailStructure, BaseOffense> {

    @Override
    public BaseOffense convert(final BaseOffenceDetailStructure baseOffenceDetailStructure) {

        return new BaseOffense.Builder().
                withOffenceSequenceNumber(baseOffenceDetailStructure.getOffenceSequenceNumber()).
                withOffenceCode(baseOffenceDetailStructure.getOffenceCode()).
                withOffenceWording(baseOffenceDetailStructure.getOffenceWording()).
                withOffenceDateCode(baseOffenceDetailStructure.getOffenceTiming().getOffenceDateCode().intValue()).
                withOffenceStartTime(baseOffenceDetailStructure.getOffenceTiming().getOffenceStart().getOffenceStartTime() == null ? null : new TimeConverter().convert(baseOffenceDetailStructure.getOffenceTiming().getOffenceStart().getOffenceStartTime())).
                withOffenceDateStartDate(baseOffenceDetailStructure.getOffenceTiming().getOffenceStart().getOffenceDateStartDate().toGregorianCalendar().toZonedDateTime().toLocalDate()).
                withOffenceEndDate(getOffenceEndDate(baseOffenceDetailStructure)).
                withOffenceEndTime(getOffenceEndTime(baseOffenceDetailStructure)).
                withAlcoholLevelAmount(baseOffenceDetailStructure.getAlcoholRelatedOffence() == null ? null : baseOffenceDetailStructure.getAlcoholRelatedOffence().getAlcoholLevelAmount()).
                withAlcoholLevelMethod(baseOffenceDetailStructure.getAlcoholRelatedOffence() == null ? null : baseOffenceDetailStructure.getAlcoholRelatedOffence().getAlcoholLevelMethod()).
                withArrestDate(baseOffenceDetailStructure.getArrestDate() == null ? null : baseOffenceDetailStructure.getArrestDate().toGregorianCalendar().toZonedDateTime().toLocalDate()).
                withChargeDate(baseOffenceDetailStructure.getChargeDate() == null ? null : baseOffenceDetailStructure.getChargeDate().toGregorianCalendar().toZonedDateTime().toLocalDate()).
                withLocationOfOffence(baseOffenceDetailStructure.getLocationOfOffence()).
                withVehicleCode(baseOffenceDetailStructure.getVehicleRelatedOffence() == null ? null : baseOffenceDetailStructure.getVehicleRelatedOffence().getVehicleCode()).
                withVehicleRegistrationMark(baseOffenceDetailStructure.getVehicleRelatedOffence() == null ? null : baseOffenceDetailStructure.getVehicleRelatedOffence().getVehicleRegistrationMark())
                .build();

    }

    private static String getOffenceEndTime(final BaseOffenceDetailStructure baseOffenceDetailStructure) {
        if (!isNull(baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd())) {
            return baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd().getOffenceEndTime() == null ? null : new TimeConverter().convert(baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd().getOffenceEndTime());
        }
        return null;
    }

    private LocalDate getOffenceEndDate(final BaseOffenceDetailStructure baseOffenceDetailStructure) {

        if (!isNull(baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd())) {
            return baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd().getOffenceEndDate() == null ?
                    null : baseOffenceDetailStructure.getOffenceTiming().getOffenceEnd().getOffenceEndDate().toGregorianCalendar().toZonedDateTime().toLocalDate();
        }
        return null;
    }

}
