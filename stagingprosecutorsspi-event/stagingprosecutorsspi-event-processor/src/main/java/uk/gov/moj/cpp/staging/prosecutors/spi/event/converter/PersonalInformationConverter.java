package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

public class PersonalInformationConverter implements Converter<PoliceIndividualDefendant, PersonalInformation> {


    private static final int UNKNOWN = 9;
    private static final int NOT_RECORDED_NOT_KNOWN = 0;

    @Override
    public PersonalInformation convert(final PoliceIndividualDefendant policeIndividualDefendant) {
        final BasePersonName personNameDetails = policeIndividualDefendant.getPersonDefendant().getBasePersonDetails().getPersonName();


        return PersonalInformation.personalInformation()
                .withObservedEthnicity(getObservedEthnicity(policeIndividualDefendant))
                .withFirstName(personNameDetails.getPersonGivenName1())
                .withLastName(personNameDetails.getPersonFamilyName())
                .withGivenName2(personNameDetails.getPersonGivenName2())
                .withGivenName3(personNameDetails.getPersonGivenName3())
                .withTitle(personNameDetails.getPersonTitle())
                .withAddress(new AddressConverter().convert(policeIndividualDefendant.getAddress()))
                .withOccupation(policeIndividualDefendant.getOccupation())
                .withOccupationCode(policeIndividualDefendant.getOccupationCode())
                .withContactDetails(new ContactDetailsConverter().convert(policeIndividualDefendant.getPersonDefendant().getBasePersonDetails()))
                .build();
    }

    private Integer getObservedEthnicity(final PoliceIndividualDefendant policeIndividualDefendant){
        if(policeIndividualDefendant.getObservedEthnicity() != null){
            return policeIndividualDefendant.getObservedEthnicity() == UNKNOWN ? NOT_RECORDED_NOT_KNOWN : policeIndividualDefendant.getObservedEthnicity();
        }
        return null;
    }
}

