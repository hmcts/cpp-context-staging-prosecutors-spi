package uk.gov.moj.cpp.staging.soap.schema.converter;

import uk.gov.dca.xmlschemas.libra.PoliceOfficerInCaseStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;

public class BasePersonNameConverter implements Converter<PoliceOfficerInCaseStructure, BasePersonName> {

    @Override
    public BasePersonName convert(PoliceOfficerInCaseStructure otherPartyOfficerInCase) {
        return new BasePersonName.Builder().
                withPersonFamilyName(otherPartyOfficerInCase.getOfficerName().getPersonFamilyName()).
                withPersonGivenName1(otherPartyOfficerInCase.getOfficerName().getPersonGivenName1()).
                withPersonGivenName2(otherPartyOfficerInCase.getOfficerName().getPersonGivenName2()).
                withPersonGivenName3(otherPartyOfficerInCase.getOfficerName().getPersonGivenName3()).
                withPersonTitle(otherPartyOfficerInCase.getOfficerName().getPersonTitle()).
                build();
    }
}
