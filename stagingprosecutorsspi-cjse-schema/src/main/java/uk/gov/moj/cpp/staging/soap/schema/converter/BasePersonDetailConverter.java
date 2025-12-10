package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.BasePersonDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;

@SuppressWarnings("squid:S1067")
public class BasePersonDetailConverter implements Converter<BasePersonDetailStructure, BasePersonDetail> {

    @Override
    public BasePersonDetail convert(BasePersonDetailStructure basePersonDetailStructure) {

        return new BasePersonDetail.Builder().
                withBirthDate(basePersonDetailStructure.getBirthdate() == null ? null : basePersonDetailStructure.getBirthdate().toGregorianCalendar().toZonedDateTime().toLocalDate()).
                withEmailAddress1(basePersonDetailStructure.getEmailDetails() == null ? null : basePersonDetailStructure.getEmailDetails().getEmailAddress1()).
                withEmailAddress2(basePersonDetailStructure.getEmailDetails() == null ? null : basePersonDetailStructure.getEmailDetails().getEmailAddress2()).
                withGender((int) basePersonDetailStructure.getGender()).
                withTelephoneNumberBusiness(basePersonDetailStructure.getTelephoneDetails() == null ? null : basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberBusiness()).
                withTelephoneNumberHome(basePersonDetailStructure.getTelephoneDetails() == null ? null : basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberHome()).
                withTelephoneNumberMobile(basePersonDetailStructure.getTelephoneDetails() == null ? null : basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberMobile()).
                withPersonName(getBasePersonName(basePersonDetailStructure.getPersonName()))
                .build();

    }

    private BasePersonName getBasePersonName(final BasePersonNameStructure personName) {
        return new BasePersonName.Builder().withPersonFamilyName(personName.getPersonFamilyName())
                .withPersonGivenName1(personName.getPersonGivenName1()).
                        withPersonGivenName2(personName.getPersonGivenName2()).
                        withPersonGivenName3(personName.getPersonGivenName3()).
                        withPersonTitle(personName.getPersonTitle())
                .build();
    }

}
