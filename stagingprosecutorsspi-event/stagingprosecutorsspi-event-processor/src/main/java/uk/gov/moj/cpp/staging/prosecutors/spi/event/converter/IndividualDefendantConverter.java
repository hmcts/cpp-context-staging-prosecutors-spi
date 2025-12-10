package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.GenderType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceParentGuardian;

import java.util.Optional;

public class IndividualDefendantConverter implements Converter<PoliceIndividualDefendant, Individual> {

    private static final int UNKNOWN = 9;
    private static final String NOT_RECORDED_NOT_KNOWN = "0";

    @Override
    public Individual convert(final PoliceIndividualDefendant policeIndividualDefendant) {


        return policeIndividualDefendant == null ? null : Individual.individual()
                .withDriverNumber(policeIndividualDefendant.getDriverNumber())
                .withDriverLicenceCode(policeIndividualDefendant.getDriverLicenceCode())
                .withPersonalInformation(new PersonalInformationConverter().convert(policeIndividualDefendant))
                .withSelfDefinedInformation(new SelfDefinedInformationConverter().convert(policeIndividualDefendant))
                .withOffenderCode(policeIndividualDefendant.getOffenderCode())
                .withParentGuardianInformation(buildParentGuardian(policeIndividualDefendant))
                .withPerceivedBirthYear(policeIndividualDefendant.getPerceivedBirthYear())
                .withBailConditions(getBailConditions(policeIndividualDefendant))
                .build();
    }

    private String getBailConditions(final PoliceIndividualDefendant policeIndividualDefendant) {
        return policeIndividualDefendant.getPersonDefendant()!=null?policeIndividualDefendant.getPersonDefendant().getBailConditions():null;
    }

    private ParentGuardianInformation buildParentGuardian(final PoliceIndividualDefendant policeIndividualDefendant) {

        final PoliceParentGuardian parentGuardianDetails = policeIndividualDefendant.getParentGuardianDetails();
        if (null == parentGuardianDetails) {
            return null;
        }



        return ParentGuardianInformation.parentGuardianInformation()
                .withDateOfBirth(parentGuardianDetails.getBasePersonDetails().getBirthDate())
                .withGender(getGender(parentGuardianDetails))
                .withObservedEthnicity(getObservedEthnicity(parentGuardianDetails))
                .withSelfDefinedEthnicity(parentGuardianDetails.getSelfDefinedEthnicity())
                .withPersonalInformation(PersonalInformation.personalInformation()
                        .withTitle(getTitle(parentGuardianDetails))
                        .withFirstName(getFirstName(parentGuardianDetails))
                        .withGivenName2(getGivenName2(parentGuardianDetails))
                        .withGivenName3(getGivenName3(parentGuardianDetails))
                        .withLastName(getLastName(parentGuardianDetails))
                        .withAddress(getAddress(parentGuardianDetails))
                        .withContactDetails(buildContactDetails(parentGuardianDetails.getBasePersonDetails()))
                        .build())
                .build();
    }

    private String  getObservedEthnicity(final PoliceParentGuardian policeParentGuardian){

        if(policeParentGuardian.getObservedEthnicity() != null) {
            return policeParentGuardian.getObservedEthnicity() == UNKNOWN ? NOT_RECORDED_NOT_KNOWN : policeParentGuardian.getObservedEthnicity().toString();
        }
        return null;
    }

    private ContactDetails buildContactDetails(final BasePersonDetail personDetail) {
        return ContactDetails.contactDetails().
                withHome(personDetail.getTelephoneNumberHome())
                .withMobile(personDetail.getTelephoneNumberMobile())
                .withPrimaryEmail(personDetail.getEmailAddress1())
                .withSecondaryEmail(personDetail.getEmailAddress2())
                .build();
    }

    private String getFirstName(final PoliceParentGuardian parentGuardianDetails) {
        return parentGuardianDetails.getBasePersonDetails() != null ? parentGuardianDetails.getBasePersonDetails().getPersonName().getPersonGivenName1() : null;
    }

    private String getGivenName2(final PoliceParentGuardian parentGuardianDetails) {
        return parentGuardianDetails.getBasePersonDetails() != null ? parentGuardianDetails.getBasePersonDetails().getPersonName().getPersonGivenName2() : null;
    }

    private String getGivenName3(final PoliceParentGuardian parentGuardianDetails) {
        return parentGuardianDetails.getBasePersonDetails() != null ? parentGuardianDetails.getBasePersonDetails().getPersonName().getPersonGivenName3() : null;
    }

    private String getLastName(final PoliceParentGuardian parentGuardianDetails) {
        return parentGuardianDetails.getBasePersonDetails() != null ? parentGuardianDetails.getBasePersonDetails().getPersonName().getPersonFamilyName() : null;
    }

    private Address getAddress(final PoliceParentGuardian parentGuardianDetails) {
        return parentGuardianDetails.getAddress() != null ? new AddressConverter().convert(parentGuardianDetails.getAddress()) : null;
    }

    private String getTitle(final PoliceParentGuardian parentGuardianDetails) {
        return (parentGuardianDetails.getBasePersonDetails() != null && parentGuardianDetails.getBasePersonDetails().getPersonName() != null) ? parentGuardianDetails.getBasePersonDetails().getPersonName().getPersonTitle() : null;
    }

    private Gender getGender(final PoliceParentGuardian parentGuardianDetails) {
        Gender gender = null;
        final Optional<Gender> optionalGender = GenderType.valueFor(parentGuardianDetails.getBasePersonDetails().getGender());
        if (optionalGender.isPresent()) {
            gender = optionalGender.get();
        }
        return gender;
    }


}

