package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase;


public class PoliceOfficerInCaseConverter implements Converter<PoliceOfficerInCase,
        uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerInCase> {

    @Override
    public uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerInCase convert(final PoliceOfficerInCase source) {

        final Address address = new AddressConverter().convert(source.getStructuredAddress());
        final ContactDetails contactDetails = ContactDetails.contactDetails()
                .withHome(source.getTelephoneNumberHome())
                .withMobile(source.getTelephoneNumberMobile())
                .withWork(source.getTelephoneNumberBusiness())
                .withPrimaryEmail(source.getEmailAddress1())
                .withSecondaryEmail(source.getEmailAddress2())
                .build();

        final PersonalInformation personalInformation = PersonalInformation.personalInformation()
                .withAddress(address)
                .withContactDetails(contactDetails)
                .withFirstName(source.getOfficerName() == null ? null : source.getOfficerName().getPersonGivenName1())
                .withLastName(source.getOfficerName() == null ? null : source.getOfficerName().getPersonFamilyName())
                .withTitle(source.getOfficerName() == null ? null : source.getOfficerName().getPersonTitle())
                .build();

        return uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerInCase.policeOfficerInCase()
                .withPoliceOfficerRank(source.getPoliceOfficerRank())
                .withPersonalInformation(personalInformation)
                .build();
    }
}
