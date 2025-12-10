package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.dca.xmlschemas.libra.PoliceIndividualDefendantStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

import static uk.gov.moj.cpp.staging.soap.schema.converter.ObservedEthnicity.getObservedEthnicity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("squid:S1067")
public class PoliceIndividualDefendantConverter implements Converter<PoliceIndividualDefendantStructure, PoliceIndividualDefendant> {

    @Override
    public PoliceIndividualDefendant convert(final PoliceIndividualDefendantStructure policeIndividualDefendantStructure) {
        final List<BasePersonName> personNameStructures = new ArrayList<>();
        if (null != policeIndividualDefendantStructure.getAlias()) {
            getBasePersonNameStructure(policeIndividualDefendantStructure, personNameStructures);
        }

        return new PoliceIndividualDefendant.Builder().
                withPersonDefendant(new BasePersonDefendantConverter().convert(policeIndividualDefendantStructure.getPersonDefendant())).
                withAddress(new AddressConverter().convert(policeIndividualDefendantStructure.getAddress())).
                withAlias(personNameStructures).
                withPersonDefendant(new BasePersonDefendantConverter().convert(policeIndividualDefendantStructure.getPersonDefendant())).
                withCounterpartIssue(policeIndividualDefendantStructure.getCounterpartIssue()).
                withCrOnumber(policeIndividualDefendantStructure.getCROnumber()).
                withCustodyStatus(policeIndividualDefendantStructure.getCustodyStatus()).
                withDocumentationLanguage(policeIndividualDefendantStructure.getLanguageOptions().getDocumentationLanguage()).
                withHearingLanguage(policeIndividualDefendantStructure.getLanguageOptions().getHearingLanguage()).
                withParentGuardianDetails(policeIndividualDefendantStructure.getParentGuardianDetails() == null ? null : new PoliceParentGuardianConverter().convert(policeIndividualDefendantStructure.getParentGuardianDetails())).
                withDriverLicenceCode(policeIndividualDefendantStructure.getDriverInformation() == null ? null : policeIndividualDefendantStructure.getDriverInformation().getDriverLicenceCode()).
                withDriverLicenceIssue(policeIndividualDefendantStructure.getDriverInformation() == null ? null : policeIndividualDefendantStructure.getDriverInformation().getDriverLicenceIssue()).
                withObservedEthnicity(getObservedEthnicity(policeIndividualDefendantStructure.getPersonEthnicity())).
                withSelfDefinedEthnicity(policeIndividualDefendantStructure.getPersonEthnicity() == null ? null : policeIndividualDefendantStructure.getPersonEthnicity().getSelfDefinedEthnicity()).
                withLanguageNeeds(policeIndividualDefendantStructure.getLanguageNeeds()).
                withDriverNumber(policeIndividualDefendantStructure.getDriverInformation() == null ? null : policeIndividualDefendantStructure.getDriverInformation().getDriverNumber()).
                withOccupation(policeIndividualDefendantStructure.getOccupation()).
                withOccupationCode(policeIndividualDefendantStructure.getOccupationCode() == null ? null : policeIndividualDefendantStructure.getOccupationCode().intValue()).
                withOffenderCode(policeIndividualDefendantStructure.getOffenderCode()).
                withPerceivedBirthYear(policeIndividualDefendantStructure.getPerceivedBirthYear() == null ? null : policeIndividualDefendantStructure.getPerceivedBirthYear().toString()).
                withSpecialNeeds(policeIndividualDefendantStructure.getSpecialNeeds()).
                build();

    }


    private void getBasePersonNameStructure(final PoliceIndividualDefendantStructure policeIndividualDefendantStructure, final List<BasePersonName> personNameStructures) {
        for (final BasePersonNameStructure personNameStructure : policeIndividualDefendantStructure.getAlias()) {
            final BasePersonName basePersonName = new BasePersonName.Builder().
                    withPersonTitle(personNameStructure.getPersonTitle()).
                    withPersonFamilyName(personNameStructure.getPersonFamilyName()).
                    withPersonGivenName1(personNameStructure.getPersonGivenName1()).
                    withPersonGivenName2(personNameStructure.getPersonGivenName2()).
                    withPersonGivenName3(personNameStructure.getPersonGivenName3()).
                    withPersonTitle(personNameStructure.getPersonTitle()).
                    build();

            personNameStructures.add(basePersonName);
        }
    }

}
