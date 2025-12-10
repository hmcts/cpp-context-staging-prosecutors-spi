package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;

public class ContactDetailsConverter implements Converter<BasePersonDetail, ContactDetails> {

    @Override
    public ContactDetails convert(final BasePersonDetail basePersonDetails) {
        return ContactDetails.contactDetails()
                .withHome(basePersonDetails.getTelephoneNumberHome())
                .withMobile(basePersonDetails.getTelephoneNumberMobile())
                .withWork(basePersonDetails.getTelephoneNumberBusiness())
                .withPrimaryEmail(basePersonDetails.getEmailAddress1())
                .withSecondaryEmail(basePersonDetails.getEmailAddress2())
                .build();
    }
}

