package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import uk.gov.dca.xmlschemas.libra.StdProsPoliceResultedCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;

import javax.inject.Inject;
import java.util.Map;

public class ResultConverter {

    private static final String RESULTED_CASES_FOR_THE_POLICE_DEFAULT_VALUE = "ResultedCasesForThePolice";
    private static final String LIBRA_STANDARD_PROSECUTOR_POLICE_DEFAULT_VALUE = "LibraStandardProsecutorPolice";
    private static final String SCHEMA_VERSION_DEFAULT_VALUE = "0.6g";

    @Inject
    private CourtSession courtSession;

    public StdProsPoliceResultedCaseStructure convert(final PublicPoliceResultGenerated source, final Map<String, Object> context) {
        final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure = new StdProsPoliceResultedCaseStructure();
        stdProsPoliceResultedCaseStructure.setSession(courtSession.buildCourtSession(source, context));
        stdProsPoliceResultedCaseStructure.setInterface(LIBRA_STANDARD_PROSECUTOR_POLICE_DEFAULT_VALUE);
        stdProsPoliceResultedCaseStructure.setFlow(RESULTED_CASES_FOR_THE_POLICE_DEFAULT_VALUE);
        stdProsPoliceResultedCaseStructure.setSchemaVersion(SCHEMA_VERSION_DEFAULT_VALUE);
        return stdProsPoliceResultedCaseStructure;
    }
}
