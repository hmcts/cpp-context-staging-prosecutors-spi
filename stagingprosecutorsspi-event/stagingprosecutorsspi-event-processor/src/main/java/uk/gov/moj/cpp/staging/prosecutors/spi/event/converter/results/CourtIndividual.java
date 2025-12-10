package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBasePersonDetailStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildCourtAddressStructure;

import uk.gov.dca.xmlschemas.libra.BasePersonDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtIndividualDefendantStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AssociatedIndividual;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.IndividualDefendant;

import java.util.Optional;

public class CourtIndividual {
    private static final int BAILCONDITIONS_MAX_LENGTH = 2500;
    private static final int START_INDEX = 0;
    private static final String PARENT_GUARDIAN = "parentGuardian";
    public static final String BAIL_STATUS_NOT_APPLICABLE = "A";

    public CourtIndividualDefendantStructure buildCourtIndividualDefendantStructure(final CaseDefendant caseDefendant) {
        final IndividualDefendant individualDefendant = caseDefendant.getIndividualDefendant();

        final CourtIndividualDefendantStructure courtIndividualDefendantStructure = new CourtIndividualDefendantStructure();

        courtIndividualDefendantStructure.setBailStatus(individualDefendant.getBailStatus() == null ? BAIL_STATUS_NOT_APPLICABLE : individualDefendant.getBailStatus().getCode());

        courtIndividualDefendantStructure.setAddress(buildCourtAddressStructure(individualDefendant.getPerson().getAddress()));
        courtIndividualDefendantStructure.setPersonDefendant(buildBasePersonDefendantStructure(individualDefendant.getPerson(), individualDefendant.getBailConditions(), caseDefendant.getPncId()));
        courtIndividualDefendantStructure.setPresentAtHearing(individualDefendant.getPresentAtHearing());
        if (null != individualDefendant.getReasonForBailConditionsOrCustody()) {
            courtIndividualDefendantStructure.setReasonForBailConditionsOrCustody(individualDefendant.getReasonForBailConditionsOrCustody());
        }

        if (null != caseDefendant.getAssociatedPerson()) {
            final Optional<AssociatedIndividual> parentGuardian = caseDefendant.getAssociatedPerson().stream().filter(a -> a.getRole().equalsIgnoreCase(PARENT_GUARDIAN)).findFirst();
            if (parentGuardian.isPresent()) {
                courtIndividualDefendantStructure.setParentGuardianDetails(new CourtParentGuardian().buildParentGuardian(parentGuardian.get()));
            }
        }
        return courtIndividualDefendantStructure;
    }

    private BasePersonDefendantStructure buildBasePersonDefendantStructure(final Individual person, final String bailConditions, final String pncId) {
        final BasePersonDefendantStructure basePersonDefendantStructure = new BasePersonDefendantStructure();
        if(nonNull(bailConditions)) {
            final String bailCondtionStr = bailConditions.substring(START_INDEX, Math.min(bailConditions.length(), BAILCONDITIONS_MAX_LENGTH));
            if(bailCondtionStr.endsWith(";")) {
                basePersonDefendantStructure.setBailConditions(bailCondtionStr.substring(0,bailCondtionStr.length() - 1));
            } else {
                basePersonDefendantStructure.setBailConditions(bailCondtionStr);
            }
        }
        basePersonDefendantStructure.setPNCidentifier(pncId);
        basePersonDefendantStructure.setBasePersonDetails(buildBasePersonDetailStructure(person));
        return basePersonDefendantStructure;
    }
}
