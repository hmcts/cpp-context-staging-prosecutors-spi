package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.PoliceCaseStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;

public class SpiCaseConverter implements Converter<PoliceCaseStructure, PoliceCase> {

    @Override
    public PoliceCase convert(final PoliceCaseStructure source) {

        return new PoliceCase.Builder().withCaseDetails(new CaseDetailsConverter().convert(source)).
                withDefendants(new PoliceDefendantConverter().convert(source)).
                withOtherPartyOfficerInCase(new PoliceOfficerInCaseConverter().convert(source)).
                build();
    }

}
