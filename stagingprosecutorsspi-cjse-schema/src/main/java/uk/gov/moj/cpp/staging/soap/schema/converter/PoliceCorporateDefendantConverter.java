package uk.gov.moj.cpp.staging.soap.schema.converter;

import uk.gov.dca.xmlschemas.libra.PoliceCorporateDefendantStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCorporateDefendant;

public class PoliceCorporateDefendantConverter implements Converter<PoliceCorporateDefendantStructure, PoliceCorporateDefendant> {

    @Override
    public PoliceCorporateDefendant convert(final PoliceCorporateDefendantStructure policeCorporateDefendantStructure) {
        return new PoliceCorporateDefendant.Builder().
                withOrganisationName(policeCorporateDefendantStructure.getOrganisationName().getOrganisationName()).
                withTelephoneNumberBusiness(policeCorporateDefendantStructure.getTelephoneNumberBusiness()).
                withEmailAddress1(policeCorporateDefendantStructure.getEmailDetails() == null ? null : policeCorporateDefendantStructure.getEmailDetails().getEmailAddress1()).
                withEmailAddress2(policeCorporateDefendantStructure.getEmailDetails() == null ? null : policeCorporateDefendantStructure.getEmailDetails().getEmailAddress2()).
                withPnCidentifier(policeCorporateDefendantStructure.getPNCidentifier()).
                withAddress(new AddressConverter().convert(policeCorporateDefendantStructure.getAddress())).
                withCrOnumber(policeCorporateDefendantStructure.getCROnumber()).
                withAlias(policeCorporateDefendantStructure.getAlias()).
                build();
    }

}
