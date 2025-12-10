package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBaseEmailDetailsStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildCourtAddressStructure;

import uk.gov.dca.xmlschemas.libra.BaseEmailDetailStructure;
import uk.gov.dca.xmlschemas.libra.BaseOrganisationDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtCorporateDefendantStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OrganisationDetails;

import java.util.Optional;

public class CourtCorporateDefendant {

    public CourtCorporateDefendantStructure buildCourtCorporateDefendantStructure(final OrganisationDetails organisation, final String bailStatus, final String pncId) {

        final CourtCorporateDefendantStructure courtCorporateDefendantStructure = new CourtCorporateDefendantStructure();
        courtCorporateDefendantStructure.setAddress(buildCourtAddressStructure(organisation.getAddress()));
        courtCorporateDefendantStructure.setBailStatus(bailStatus);
        courtCorporateDefendantStructure.setPNCidentifier(pncId);
        final Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(organisation.getContact());
        baseEmailDetailStructure.ifPresent(courtCorporateDefendantStructure::setEmailDetails);

        if (nonNull(organisation.getContact()) && nonNull(organisation.getContact().getWork())) {
            courtCorporateDefendantStructure.setTelephoneNumberBusiness(organisation.getContact().getWork());
        }

        final BaseOrganisationDefendantStructure baseOrganisationDefendantStructure = new BaseOrganisationDefendantStructure();
        baseOrganisationDefendantStructure.setOrganisationName(organisation.getName());
        courtCorporateDefendantStructure.setOrganisationName(baseOrganisationDefendantStructure);
        courtCorporateDefendantStructure.setPresentAtHearing(organisation.getPresentAtHearing());

        return courtCorporateDefendantStructure;
    }
}