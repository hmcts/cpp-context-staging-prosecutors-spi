package uk.gov.moj.cpp.staging.soap.schema.converter;

import static uk.gov.moj.cpp.staging.soap.schema.converter.ObservedEthnicity.getObservedEthnicity;

import uk.gov.dca.xmlschemas.libra.PoliceVictimStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceVictim;

import java.time.LocalDate;

@SuppressWarnings("squid:S1067")
public class PoliceVictimConverter implements Converter<PoliceVictimStructure, PoliceVictim> {

    @Override
    public PoliceVictim convert(final PoliceVictimStructure policeVictimStructure) {
        return new PoliceVictim.Builder().
                withAddress(new AddressConverter().convert(policeVictimStructure.getAddress())).
                withBirthDate(getBirthDate(policeVictimStructure)).
                withEmailAddress1(getEmailAddress1(policeVictimStructure)).
                withEmailAddress2(getEmailAddress2(policeVictimStructure)).
                withGender(getGender(policeVictimStructure)).
                withObservedEthnicity( getObservedEthnicity(policeVictimStructure.getPersonEthnicity())).
                withSelfDefinedEthnicity(getSelfDefinedEthnicity(policeVictimStructure)).
                withPersonFamilyName(getPersonFamilyName(policeVictimStructure)).
                withPersonGivenName1(getPersonGivenName1(policeVictimStructure)).
                withPersonGivenName2(getPersonGivenName2(policeVictimStructure)).
                withPersonGivenName3(getPersonGivenName3(policeVictimStructure)).
                withPersonTitle(getPersonTitle(policeVictimStructure)).
                withTelephoneNumberBusiness(getTelephoneNumberBusiness(policeVictimStructure)).
                withTelephoneNumberHome(getTelephoneNumberHome(policeVictimStructure)).
                withTelephoneNumberMobile(getTelephoneNumberMobile(policeVictimStructure)).
                build();
    }

    private String getTelephoneNumberMobile(final PoliceVictimStructure policeVictimStructure) {
        return isTelephoneDetailsExist(policeVictimStructure) ?policeVictimStructure.getTelephoneDetails().getTelephoneNumberMobile():null;
    }

    private String getTelephoneNumberHome(final PoliceVictimStructure policeVictimStructure) {
        return isTelephoneDetailsExist(policeVictimStructure) ?policeVictimStructure.getTelephoneDetails().getTelephoneNumberHome():null;
    }

    private String getTelephoneNumberBusiness(final PoliceVictimStructure policeVictimStructure) {
        return isTelephoneDetailsExist(policeVictimStructure) ? policeVictimStructure.getTelephoneDetails().getTelephoneNumberBusiness():null;
    }

    private String getPersonTitle(final PoliceVictimStructure policeVictimStructure) {
        return isPersonalDetailsExist(policeVictimStructure) ?policeVictimStructure.getPersonDetails().getPersonTitle():null;
    }

    private String getPersonGivenName3(final PoliceVictimStructure policeVictimStructure) {
        return isPersonalDetailsExist(policeVictimStructure) ? policeVictimStructure.getPersonDetails().getPersonGivenName3():null;
    }

    private String getPersonGivenName2(final PoliceVictimStructure policeVictimStructure) {
        return isPersonalDetailsExist(policeVictimStructure) ? policeVictimStructure.getPersonDetails().getPersonGivenName2():null;
    }

    private String getPersonGivenName1(final PoliceVictimStructure policeVictimStructure) {
        return isPersonalDetailsExist(policeVictimStructure) ? policeVictimStructure.getPersonDetails().getPersonGivenName1():null;
    }

    private String getPersonFamilyName(final PoliceVictimStructure policeVictimStructure) {
        return isPersonalDetailsExist(policeVictimStructure) ? policeVictimStructure.getPersonDetails().getPersonFamilyName():null;
    }

    private String getSelfDefinedEthnicity(final PoliceVictimStructure policeVictimStructure) {
        return policeVictimStructure.getPersonEthnicity()!=null?policeVictimStructure.getPersonEthnicity().getSelfDefinedEthnicity():null;
    }

    private LocalDate getBirthDate(final PoliceVictimStructure policeVictimStructure) {
        return policeVictimStructure.getBirthdate()!=null? policeVictimStructure.getBirthdate().toGregorianCalendar().toZonedDateTime().toLocalDate():null;
    }

    private Integer getGender(final PoliceVictimStructure policeVictimStructure) {
        return policeVictimStructure.getGender()!=null? policeVictimStructure.getGender().intValue():null;
    }

    private String getEmailAddress2(final PoliceVictimStructure policeVictimStructure) {
        return  policeVictimStructure.getEmailDetails()!=null ?policeVictimStructure.getEmailDetails().getEmailAddress2():null;
    }

    private String getEmailAddress1(final PoliceVictimStructure policeVictimStructure) {
        return  policeVictimStructure.getEmailDetails()!=null ? policeVictimStructure.getEmailDetails().getEmailAddress1():null;
    }

    private boolean isTelephoneDetailsExist(final PoliceVictimStructure policeVictimStructure) {
        return policeVictimStructure.getTelephoneDetails()!=null;
    }

    private boolean isPersonalDetailsExist(final PoliceVictimStructure policeVictimStructure) {
        return policeVictimStructure.getPersonDetails()!=null;
    }

}
