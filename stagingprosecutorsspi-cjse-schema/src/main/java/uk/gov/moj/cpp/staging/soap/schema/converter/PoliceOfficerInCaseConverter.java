package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.PoliceCaseStructure;
import uk.gov.dca.xmlschemas.libra.PoliceOfficerInCaseStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase;

@SuppressWarnings("squid:S1067")
public class PoliceOfficerInCaseConverter implements Converter<PoliceCaseStructure, PoliceOfficerInCase> {

    @Override
    public PoliceOfficerInCase convert(final PoliceCaseStructure source) {

        final PoliceOfficerInCaseStructure otherPartyOfficerInCase = source.getOtherPartyOfficerInCase();

        return new PoliceOfficerInCase.Builder().
                withDxaddress(otherPartyOfficerInCase.getDXaddress()).
                withEmailAddress1(otherPartyOfficerInCase.getEmailDetails() == null ? null : otherPartyOfficerInCase.getEmailDetails().getEmailAddress1()).
                withEmailAddress2(otherPartyOfficerInCase.getEmailDetails() == null ? null : otherPartyOfficerInCase.getEmailDetails().getEmailAddress2()).
                withOfficerName(new BasePersonNameConverter().convert(otherPartyOfficerInCase)).
                withPoliceOfficerRank(otherPartyOfficerInCase.getPoliceOfficerRank()).
                withPoliceWorkerLocationCode(otherPartyOfficerInCase.getPoliceWorkerLocationCode()).
                withPoliceWorkerReferenceNumber(otherPartyOfficerInCase.getPoliceWorkerReferenceNumber()).
                withTelephoneNumberBusiness(otherPartyOfficerInCase.getTelephoneDetails() == null ? null : otherPartyOfficerInCase.getTelephoneDetails().getTelephoneNumberBusiness()).
                withTelephoneNumberMobile(otherPartyOfficerInCase.getTelephoneDetails() == null ? null : otherPartyOfficerInCase.getTelephoneDetails().getTelephoneNumberMobile()).
                withFaxNumber(otherPartyOfficerInCase.getFAXnumber()).
                withStructuredAddress(otherPartyOfficerInCase.getStructuredAddress() == null ? null : new AddressConverter().convert(otherPartyOfficerInCase.getStructuredAddress()))
                .build();
    }

}
