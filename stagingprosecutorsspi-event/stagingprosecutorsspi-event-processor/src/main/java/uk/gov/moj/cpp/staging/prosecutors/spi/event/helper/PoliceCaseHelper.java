package uk.gov.moj.cpp.staging.prosecutors.spi.event.helper;

import org.apache.commons.lang3.StringUtils;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;

public class PoliceCaseHelper {

    private static final String A_4 = "A4";
    private static final String ZERO_FOUR = "04";
    public static final String SURREY_POLICE_ORIG_ORGANISATION = "A45AA00";
    public static final String SUSSEX_POLICE_ORIG_ORGANISATION = "A47AA00";
    private PoliceCaseHelper() {

    }

    public static PoliceCase getPoliceCaseWithSystemIdAndOrganisation(PoliceCase policeCase, String systemId) {

        return PoliceCase.policeCase().withCaseDetails(getCaseDetails(policeCase.getCaseDetails(), systemId)).
                withDefendants(policeCase.getDefendants()).
                withOtherPartyOfficerInCase(policeCase.getOtherPartyOfficerInCase()).
                build();
    }

    private static CaseDetails getCaseDetails(CaseDetails caseDetails, String systemId) {
        return new CaseDetails.Builder().
                withPtiurn(caseDetails.getPtiurn()).
                withOriginatingOrganisation(substituteOrganisationUsedBySurreyAndSussex(caseDetails.getOriginatingOrganisation())).
                withCaseInitiationCode(caseDetails.getCaseInitiationCode()).
                withSummonsCode(caseDetails.getSummonsCode()).
                withInformant(caseDetails.getInformant()).
                withCpSorganisation(caseDetails.getCpSorganisation()).
                withCaseMarker(caseDetails.getCaseMarker()).
                withInitialHearing(caseDetails.getInitialHearing()).
                withVehicleOperatorLicenceNumber(caseDetails.getVehicleOperatorLicenceNumber()).
                withPoliceSystemId(systemId).build();
    }

    private static String substituteOrganisationUsedBySurreyAndSussex(final String originalOrganisation){
        final String  trimmedValue =  StringUtils.trim(originalOrganisation);
        if(SURREY_POLICE_ORIG_ORGANISATION.equalsIgnoreCase(trimmedValue) || SUSSEX_POLICE_ORIG_ORGANISATION.equalsIgnoreCase(trimmedValue)) {
            return trimmedValue.replace(A_4, ZERO_FOUR);
        }
        return originalOrganisation;
    }
}
