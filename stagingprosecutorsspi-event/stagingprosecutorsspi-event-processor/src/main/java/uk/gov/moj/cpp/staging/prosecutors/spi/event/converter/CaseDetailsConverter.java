package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor.prosecutor;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.time.ZonedDateTime;
import java.util.List;


public class CaseDetailsConverter implements ParameterisedConverter<SpiProsecutionCaseReceived, uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails, ZonedDateTime> {
    static final int CASE_MARKER_STRING_LENGHT = 2;

    @Override
    public uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails convert(final SpiProsecutionCaseReceived source, final ZonedDateTime dateReceived) {

        final CaseDetails caseDetails = source.getPoliceCase().getCaseDetails();
        final String initiationCode = caseDetails.getCaseInitiationCode();
        final PoliceOfficerInCase policeOfficerInCase = source.getPoliceCase().getOtherPartyOfficerInCase();
        return caseDetails()
                .withProsecutorCaseReference(caseDetails.getPtiurn())
                .withOtherPartyOfficerInCase(new PoliceOfficerInCaseConverter().convert(policeOfficerInCase))
                .withCaseId(source.getCaseId())
                .withDateReceived(dateReceived.toLocalDate())
                .withSummonsCode(caseDetails.getSummonsCode())
                .withCpsOrganisation(caseDetails.getCpSorganisation())
                .withInitiationCode(initiationCode)
                .withOriginatingOrganisation(caseDetails.getOriginatingOrganisation())
                .withPoliceSystemId(source.getPoliceCase().getCaseDetails().getPoliceSystemId())
                .withCaseMarkers(isEmpty(caseDetails.getCaseMarker()) ? emptyList() : buildCaseMarkers(caseDetails.getCaseMarker().trim()))
                .withProsecutor(
                        prosecutor()
                                .withInformant(caseDetails.getInformant())
                                .withProsecutingAuthority(caseDetails.getOriginatingOrganisation())
                                .build()
                ).build();
    }

    private List<CaseMarker> buildCaseMarkers(final String caseMarkers) {
        String[] caseMarkersArray = caseMarkers.split("\\s+");

        if (caseMarkersArray.length == 1 && caseMarkers.length() > CASE_MARKER_STRING_LENGHT) {
            caseMarkersArray = spiltCaseMarkesBasedOnStringLength(caseMarkers);
        }

        return stream(caseMarkersArray)
                .map(caseMarker -> caseMarker().withMarkerTypeCode(caseMarker).build())
                .distinct()
                .collect(toList());

    }


    private String[] spiltCaseMarkesBasedOnStringLength(String caseMarkerText) {

        final int numberOfStrings = (caseMarkerText.length() + 1) / CASE_MARKER_STRING_LENGHT;
        final String[] caseMarkersArray = new String[numberOfStrings];

        for (int i = 0; i < numberOfStrings; i++) {
            final int beginIndex = i * CASE_MARKER_STRING_LENGHT;
            final int endIndex = beginIndex + CASE_MARKER_STRING_LENGHT;
            caseMarkersArray[i] = caseMarkerText.substring(beginIndex, Math.min(caseMarkerText.length(), endIndex));
        }
        return caseMarkersArray;
    }
}