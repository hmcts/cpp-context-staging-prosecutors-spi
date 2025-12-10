package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase.policeOfficerInCase;

import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiProsecutionCaseReceived;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDetailsConverterTest {

    private final static String CASE_MARKERS = "CM1    CM2  CM3    CM2 CM3";

    private final static List<String> SINGLE_CASE_MARKER_COMBINATIONS = asList(
            "AA ",
            " AA",
            " AA ",
            "AA"
    );

    private final static Map<String, List<String>> DOUBLE_CASE_MARKER_COMBINATIONS = new HashMap() {{
        put("AA BB ", asList("AA", "BB"));
        put(" AA BB", asList("AA", "BB"));
        put(" AA BB ", asList("AA", "BB"));
        put(" AABB", asList("AA", "BB"));
        put("AABB ", asList("AA", "BB"));
        put(" AABB ", asList("AA", "BB"));
        put("AA B ", asList("AA", "B"));
        put("AA XYZ ", asList("AA", "XYZ"));
        put(" AA B", asList("AA", "B"));
        put(" AA BBC", asList("AA", "BBC"));
        put(" AA B ", asList("AA", "B"));
        put(" AA BBC ", asList("AA", "BBC"));
        put("AA BB", asList("AA", "BB"));
        put("AABB", asList("AA", "BB"));

    }};

    private final static Map<String, List<String>> TRIPLE_CASE_MARKER_COMBINATIONS = new HashMap() {{
        put("AA BB CC ", asList("AA", "BB", "CC"));
        put("AA BB C ", asList("AA", "BB", "C"));
        put("AA B C ", asList("AA", "B", "C"));
        put(" AA BB C", asList("AA", "BB", "C"));
        put(" AA B C", asList("AA", "B", "C"));
        put(" AA BB C ", asList("AA", "BB", "C"));
        put(" AA B C ", asList("AA", "B", "C"));
        put("AA BB CC", asList("AA", "BB", "CC"));
        put("AABBCC", asList("AA", "BB", "CC"));
        put("AA BB CCDD", asList("AA", "BB", "CCDD"));
    }};

    private final static List<String> QUADRUPLE_CASE_MARKER_COMBINATIONS = asList("AABBCCDD");

    @Mock
    private SpiProsecutionCaseReceived spiProsecutionCaseReceived;

    @Mock
    private PoliceCase policeCase;

    private final StoppedClock clock = new StoppedClock(now());

    private final CaseDetailsConverter converter = new CaseDetailsConverter();

    @Test
    public void testCaseDetailsConverter() {
        final CaseDetails requestCaseDetailsObj = getMockCaseDetails(CASE_MARKERS);

        when(spiProsecutionCaseReceived.getPoliceCase()).thenReturn(policeCase);
        when(policeCase.getCaseDetails()).thenReturn(requestCaseDetailsObj);
        when(policeCase.getOtherPartyOfficerInCase()).thenReturn(getMockPoliceOfficerInCase());


        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj = converter.convert(spiProsecutionCaseReceived, clock.now());
        assertThat(transformedCaseDetailsObj.getProsecutor().getInformant(), is(requestCaseDetailsObj.getInformant()));
        assertThat(transformedCaseDetailsObj.getProsecutor().getProsecutingAuthority(), is(requestCaseDetailsObj.getOriginatingOrganisation()));
        assertThat(transformedCaseDetailsObj.getProsecutorCaseReference(), is(requestCaseDetailsObj.getPtiurn()));
        assertThat(transformedCaseDetailsObj.getPoliceSystemId(), is(requestCaseDetailsObj.getPoliceSystemId()));
        assertCaseMarkersDetails(transformedCaseDetailsObj);
        assertThat(transformedCaseDetailsObj.getDateReceived(), is(clock.now().toLocalDate()));
    }

    @Test
    public void testCaseDetailsConverterForSingleCaseMarkerCombinations() {
        for (String caseMarker : SINGLE_CASE_MARKER_COMBINATIONS) {
            final CaseDetails requestCaseDetailsObj = getMockCaseDetails(caseMarker);
            when(spiProsecutionCaseReceived.getPoliceCase()).thenReturn(policeCase);
            when(policeCase.getCaseDetails()).thenReturn(requestCaseDetailsObj);
            when(policeCase.getOtherPartyOfficerInCase()).thenReturn(getMockPoliceOfficerInCase());

            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj = converter.convert(spiProsecutionCaseReceived, clock.now());

            assertThat(transformedCaseDetailsObj.getProsecutor().getInformant(), is(requestCaseDetailsObj.getInformant()));
            assertThat(transformedCaseDetailsObj.getProsecutor().getProsecutingAuthority(), is(requestCaseDetailsObj.getOriginatingOrganisation()));
            assertThat(transformedCaseDetailsObj.getProsecutorCaseReference(), is(requestCaseDetailsObj.getPtiurn()));
            assertThat(transformedCaseDetailsObj.getPoliceSystemId(), is(requestCaseDetailsObj.getPoliceSystemId()));
            assertCaseMarkersDetailsForSingleCaseMarkers(transformedCaseDetailsObj);
            assertThat(transformedCaseDetailsObj.getDateReceived(), is(clock.now().toLocalDate()));

        }

    }

    @Test
    public void testCaseDetailsConverterForDoubleCaseMarkerCombinations() {
        for (Map.Entry<String, List<String>> caseMarker : DOUBLE_CASE_MARKER_COMBINATIONS.entrySet()) {
            final CaseDetails requestCaseDetailsObj = getMockCaseDetails(caseMarker.getKey());
            when(spiProsecutionCaseReceived.getPoliceCase()).thenReturn(policeCase);
            when(policeCase.getCaseDetails()).thenReturn(requestCaseDetailsObj);
            when(policeCase.getOtherPartyOfficerInCase()).thenReturn(getMockPoliceOfficerInCase());

            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj = converter.convert(spiProsecutionCaseReceived, clock.now());

            assertThat(transformedCaseDetailsObj.getProsecutor().getInformant(), is(requestCaseDetailsObj.getInformant()));
            assertThat(transformedCaseDetailsObj.getProsecutor().getProsecutingAuthority(), is(requestCaseDetailsObj.getOriginatingOrganisation()));
            assertThat(transformedCaseDetailsObj.getProsecutorCaseReference(), is(requestCaseDetailsObj.getPtiurn()));
            assertThat(transformedCaseDetailsObj.getPoliceSystemId(), is(requestCaseDetailsObj.getPoliceSystemId()));
            assertCaseMarkersDetailsForDoubleCaseMarkers(transformedCaseDetailsObj, caseMarker.getValue());
            assertThat(transformedCaseDetailsObj.getDateReceived(), is(clock.now().toLocalDate()));

        }

    }

    @Test
    public void testCaseDetailsConverterForTripleCaseMarkerCombinations() {
        for (Map.Entry<String, List<String>> caseMarker : TRIPLE_CASE_MARKER_COMBINATIONS.entrySet()) {
            final CaseDetails requestCaseDetailsObj = getMockCaseDetails(caseMarker.getKey());
            when(spiProsecutionCaseReceived.getPoliceCase()).thenReturn(policeCase);
            when(policeCase.getCaseDetails()).thenReturn(requestCaseDetailsObj);
            when(policeCase.getOtherPartyOfficerInCase()).thenReturn(getMockPoliceOfficerInCase());

            final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj = converter.convert(spiProsecutionCaseReceived, clock.now());

            assertThat(transformedCaseDetailsObj.getProsecutor().getInformant(), is(requestCaseDetailsObj.getInformant()));
            assertThat(transformedCaseDetailsObj.getProsecutor().getProsecutingAuthority(), is(requestCaseDetailsObj.getOriginatingOrganisation()));
            assertThat(transformedCaseDetailsObj.getProsecutorCaseReference(), is(requestCaseDetailsObj.getPtiurn()));
            assertThat(transformedCaseDetailsObj.getPoliceSystemId(), is(requestCaseDetailsObj.getPoliceSystemId()));
            assertCaseMarkersDetailsForTripleCaseMarkers(transformedCaseDetailsObj, caseMarker.getValue());
            assertThat(transformedCaseDetailsObj.getDateReceived(), is(clock.now().toLocalDate()));

        }

    }

    @Test
    public void testCaseDetailsConverterForQuadrupleCaseMarkerCombinations() {
            for (String caseMarker : QUADRUPLE_CASE_MARKER_COMBINATIONS) {
                final CaseDetails requestCaseDetailsObj = getMockCaseDetails(caseMarker);
                when(spiProsecutionCaseReceived.getPoliceCase()).thenReturn(policeCase);
                when(policeCase.getCaseDetails()).thenReturn(requestCaseDetailsObj);
                when(policeCase.getOtherPartyOfficerInCase()).thenReturn(getMockPoliceOfficerInCase());

                final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj = converter.convert(spiProsecutionCaseReceived, clock.now());

                assertThat(transformedCaseDetailsObj.getProsecutor().getInformant(), is(requestCaseDetailsObj.getInformant()));
                assertThat(transformedCaseDetailsObj.getProsecutor().getProsecutingAuthority(), is(requestCaseDetailsObj.getOriginatingOrganisation()));
                assertThat(transformedCaseDetailsObj.getProsecutorCaseReference(), is(requestCaseDetailsObj.getPtiurn()));
                assertThat(transformedCaseDetailsObj.getPoliceSystemId(), is(requestCaseDetailsObj.getPoliceSystemId()));
                assertCaseMarkersDetailsForQuadrupleCaseMarkers(transformedCaseDetailsObj);
                assertThat(transformedCaseDetailsObj.getDateReceived(), is(clock.now().toLocalDate()));

            }

        }


    private CaseDetails getMockCaseDetails(String caseMarker) {
        return caseDetails()
                .withPtiurn("URN")
                .withOriginatingOrganisation("OriginatingOrganisation")
                .withInformant("informant")
                .withCaseMarker(caseMarker)
                .withPoliceSystemId("00301PoliceCaseSystem")
                .withInitialHearing(new SpiInitialHearing("WestMinister", LocalDate.now(), "12:00"))
                .build();
    }

    private PoliceOfficerInCase getMockPoliceOfficerInCase() {
        return policeOfficerInCase()
                .withStructuredAddress(getAddress())
                .withPoliceOfficerRank("Sergeant")
                .build();
    }

    private Address getAddress() {
        return address()
                .withPaon("682 Essex Electrical Supplies")
                .withStreetDescription("Green Lane")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

    private void assertCaseMarkersDetails(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj) {
        final String[] caseMarkers = CASE_MARKERS.split("\\s+");
        final int caseMarkersSizeWithoutDuplicates = 3;
        assertThat(transformedCaseDetailsObj.getCaseMarkers().size(), is(caseMarkersSizeWithoutDuplicates));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(0).getMarkerTypeCode(), is(caseMarkers[0]));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(1).getMarkerTypeCode(), is(caseMarkers[1]));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(2).getMarkerTypeCode(), is(caseMarkers[2]));
    }

    private void assertCaseMarkersDetailsForSingleCaseMarkers(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj) {
        assertThat(transformedCaseDetailsObj.getCaseMarkers().size(), is(1));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(0).getMarkerTypeCode(), is("AA"));
    }

    private void assertCaseMarkersDetailsForDoubleCaseMarkers(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj, final List<String> expectedCaseMarkerList) {
        assertThat(transformedCaseDetailsObj.getCaseMarkers().size(), is(2));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(0).getMarkerTypeCode(), is(expectedCaseMarkerList.get(0)));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(1).getMarkerTypeCode(), is(expectedCaseMarkerList.get(1)));

    }

    private void assertCaseMarkersDetailsForTripleCaseMarkers(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj, final List<String> expectedCaseMarkerList) {
        assertThat(transformedCaseDetailsObj.getCaseMarkers().size(), is(3));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(0).getMarkerTypeCode(), is(expectedCaseMarkerList.get(0)));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(1).getMarkerTypeCode(), is(expectedCaseMarkerList.get(1)));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(2).getMarkerTypeCode(), is(expectedCaseMarkerList.get(2)));

    }

    private void assertCaseMarkersDetailsForQuadrupleCaseMarkers(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails transformedCaseDetailsObj) {
        assertThat(transformedCaseDetailsObj.getCaseMarkers().size(), is(4));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(0).getMarkerTypeCode(), is("AA"));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(1).getMarkerTypeCode(), is("BB"));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(2).getMarkerTypeCode(), is("CC"));
        assertThat(transformedCaseDetailsObj.getCaseMarkers().get(3).getMarkerTypeCode(), is("DD"));
    }

}