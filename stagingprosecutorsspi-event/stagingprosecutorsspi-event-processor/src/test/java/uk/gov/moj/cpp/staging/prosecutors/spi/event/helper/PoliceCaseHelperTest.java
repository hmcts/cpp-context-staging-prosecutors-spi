package uk.gov.moj.cpp.staging.prosecutors.spi.event.helper;


import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PoliceCaseHelper.SURREY_POLICE_ORIG_ORGANISATION;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PoliceCaseHelper.SUSSEX_POLICE_ORIG_ORGANISATION;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PoliceCaseHelperTest {

    private static final String POLICE_SYSTEM_ID = "00301PoliceCaseSystem";
    private static final String SWAPPED_SURREY_ORGANISATION = "045AA00";
    private static final String SWAPPED_SUSSEX_ORGANISATION = "047AA00";

    @InjectMocks
    private PoliceCaseHelper policeCaseHelper;
    @Mock
    private PoliceCase policeCase;

    @Test
    public void shouldReturnPoliceCaseWithSystemIdAndSwappedOrganisationForSurrey() {
        when(policeCase.getCaseDetails()).thenReturn(getMockCaseDetails(SURREY_POLICE_ORIG_ORGANISATION));
        PoliceCase policeCaseWithSystemId = policeCaseHelper.getPoliceCaseWithSystemIdAndOrganisation(policeCase, POLICE_SYSTEM_ID);
        assertThat(policeCaseWithSystemId.getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(policeCaseWithSystemId.getCaseDetails().getOriginatingOrganisation(), is(SWAPPED_SURREY_ORGANISATION));
    }

    @Test
    public void shouldReturnPoliceCaseWithSystemIdAndSwappedOrganisationForSussex() {
        when(policeCase.getCaseDetails()).thenReturn(getMockCaseDetails(SUSSEX_POLICE_ORIG_ORGANISATION));
        PoliceCase policeCaseWithSystemId = policeCaseHelper.getPoliceCaseWithSystemIdAndOrganisation(policeCase, POLICE_SYSTEM_ID);
        assertThat(policeCaseWithSystemId.getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(policeCaseWithSystemId.getCaseDetails().getOriginatingOrganisation(), is(SWAPPED_SUSSEX_ORGANISATION));
    }
    @Test
    public void shouldReturnPoliceCaseWithSystemIdAndSameOrganisation() {
        final String anyOrganisation = "anyorganisation";
        when(policeCase.getCaseDetails()).thenReturn(getMockCaseDetails(anyOrganisation));
        PoliceCase policeCaseWithSystemId = policeCaseHelper.getPoliceCaseWithSystemIdAndOrganisation(policeCase, POLICE_SYSTEM_ID);
        assertThat(policeCaseWithSystemId.getCaseDetails().getPoliceSystemId(), is(POLICE_SYSTEM_ID));
        assertThat(policeCaseWithSystemId.getCaseDetails().getOriginatingOrganisation(), is(anyOrganisation));
    }


    private CaseDetails getMockCaseDetails(final String organisation) {
        return CaseDetails.caseDetails()
                .withPtiurn("URN")
                .withOriginatingOrganisation(organisation)
                .withInformant("informant")
                .withInitialHearing(new SpiInitialHearing("WestMinister", LocalDate.now(), "12:00"))
                .build();
    }
}
