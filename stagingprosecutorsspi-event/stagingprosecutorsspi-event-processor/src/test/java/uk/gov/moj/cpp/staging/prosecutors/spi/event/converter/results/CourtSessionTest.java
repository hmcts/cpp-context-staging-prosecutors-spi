package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.createPublicPoliceResultGenerated;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dca.xmlschemas.libra.CourtCaseStructure;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.dca.xmlschemas.libra.CourtSessionStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;


@ExtendWith(MockitoExtension.class)
public class CourtSessionTest {
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    private CourtDefendant courtDefendant;

    @Spy
    private CourtOffence courtOffence;

    @Spy
    private CourtCase courtCase;

    @Spy
    private CourtHearingSession courtHearingSession;

    @InjectMocks
    private CourtSession courtSession;

    @Test
    public void testBuildCourtSession() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);
        setField(courtDefendant, "courtOffence", courtOffence);
        setField(courtCase, "courtDefendant", courtDefendant);

        PublicPoliceResultGenerated publicPoliceResultGenerated = createPublicPoliceResultGenerated();
        final HashMap<String, Object> context = new HashMap<>();

        CourtSessionStructure courtSessionStructure =  courtSession.buildCourtSession(publicPoliceResultGenerated, context);

        assertNotNull(courtSessionStructure.getCourtHearing());
        assertThat(courtSessionStructure.getCourtHearing().getPSAcode(), is(valueOf(publicPoliceResultGenerated.getCourtCentreWithLJA().getPsaCode())));
        assertThat(courtSessionStructure.getCourtHearing().getHearing().getCourtHearingLocation(), is(publicPoliceResultGenerated.getCourtCentreWithLJA().getCourtHearingLocation()));
        assertNotNull(courtSessionStructure.getCase());
        for(CourtCaseStructure courtCaseStructure: courtSessionStructure.getCase()){
           assertThat(courtCaseStructure.getPTIURN(), is(publicPoliceResultGenerated.getUrn()));
            assertThat(courtCaseStructure.getDefendant().getProsecutorReference(), is(publicPoliceResultGenerated.getDefendant().getProsecutorReference()));
            for(CourtOffenceStructure courtOffenceStructure: courtCaseStructure.getDefendant().getOffence()){
                assertThat(courtOffenceStructure.getInitiatedDate(), is(getXmlGregorianCalendar(publicPoliceResultGenerated.getDefendant().getOffences().get(0).getStartDate())));
            }
        }
    }
}
