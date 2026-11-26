package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import uk.gov.dca.xmlschemas.libra.CourtDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

public class CourtDefendant {

    private static final String BAIL_STATUS_NOT_APPLICABLE = "A";

    @Inject
    private CourtOffence courtOffence;

    public CourtDefendantStructure buildCourtDefendantStructure(final CaseDefendant caseDefendant, final Integer psaCode, final Map<String, Object> context, final XMLGregorianCalendar dateOfHearing) {
        final CourtDefendantStructure courtDefendantStructure = new CourtDefendantStructure();
        courtDefendantStructure.setProsecutorReference(caseDefendant.getProsecutorReference());
        if (null != caseDefendant.getIndividualDefendant()) {
            courtDefendantStructure.setCourtIndividualDefendant(new CourtIndividual().buildCourtIndividualDefendantStructure(caseDefendant));
        }
        if (null != caseDefendant.getCorporateDefendant()) {
            courtDefendantStructure.setCourtCorporateDefendant(new CourtCorporateDefendant().buildCourtCorporateDefendantStructure(caseDefendant.getCorporateDefendant(), BAIL_STATUS_NOT_APPLICABLE, caseDefendant.getPncId()));
        }
        courtDefendantStructure.getOffence().addAll(buildCourtOffenceStructureList(caseDefendant.getOffences(), psaCode, context, dateOfHearing));
        return courtDefendantStructure;
    }

    private List<CourtOffenceStructure> buildCourtOffenceStructureList(final List<OffenceDetails> offences, final Integer psaCode, final Map<String, Object> context, final XMLGregorianCalendar dateOfHearing) {
        final List<CourtOffenceStructure> courtOffenceStructuresList = new ArrayList<>();
        offences.stream()
                .filter(o -> isNotEmpty(o.getJudicialResults()))
                .forEach(offenceDetails -> courtOffenceStructuresList.add(courtOffence.buildCourtOffenceStructure(offenceDetails, psaCode, context, dateOfHearing)));
        return courtOffenceStructuresList;
    }

}
