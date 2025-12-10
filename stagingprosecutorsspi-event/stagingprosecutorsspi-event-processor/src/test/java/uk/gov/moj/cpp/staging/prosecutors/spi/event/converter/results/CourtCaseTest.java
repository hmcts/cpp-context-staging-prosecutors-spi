package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendarFromLocalDate;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.HEARING_DATE;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dca.xmlschemas.libra.CourtCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate;

import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
public class CourtCaseTest {

    private static final String URN = "URN123456";
    private static final Integer PSA_CODE = 1010;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    private CourtDefendant courtDefendant;

    @Spy
    private CourtOffence courtOffence;

    @InjectMocks
    private CourtCase courtCase;

    @Test
    public void testBuildCourtCaseStructure() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));

        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);
        setField(courtDefendant, "courtOffence", courtOffence);

        final PublicPoliceResultGenerated publicPoliceResultGenerated = new PublicPoliceResultGenerated();
        publicPoliceResultGenerated.setDefendant(TestTemplate.buildCaseDefendant());
        publicPoliceResultGenerated.setUrn(URN);
        final HashMap<String, Object> context = new HashMap<>();

        final CourtCaseStructure courtCaseStructure = courtCase.buildCourtCaseStructure(publicPoliceResultGenerated, PSA_CODE, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertNotNull(courtCaseStructure.getDefendant());

        assertThat(courtCaseStructure.getDefendant().getProsecutorReference(), is(publicPoliceResultGenerated.getDefendant().getProsecutorReference()));
        assertThat(courtCaseStructure.getPTIURN(), is(publicPoliceResultGenerated.getUrn()));

    }

}



