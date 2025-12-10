package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendarFromLocalDate;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.HEARING_DATE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import uk.gov.dca.xmlschemas.libra.CourtDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.justice.core.courts.Address;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDefendantTest
{
    private static final Integer PSA_CODE = 1010;
    private static final String NOT_APPLICABLE_BAIL_STATUS = "A";

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    private CourtOffence courtOffence;

    @InjectMocks
    private CourtDefendant courtDefendant;

    @Test
    public void testBuildCourtDefendantStructure () {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);

        CaseDefendant caseDefendant = TestTemplate.buildCaseDefendant();
        Address primaryAddress = caseDefendant.getCorporateDefendant().getAddress();
        final HashMap<String, Object> context = new HashMap<>();

        CourtDefendantStructure  courtDefendantStructure =  courtDefendant.buildCourtDefendantStructure(caseDefendant, PSA_CODE, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertNotNull(courtDefendantStructure);
        assertThat(courtDefendantStructure.getProsecutorReference(), is(caseDefendant.getProsecutorReference()));

        assertThat(courtDefendantStructure.getCourtCorporateDefendant().getBailStatus(), is(NOT_APPLICABLE_BAIL_STATUS));
        assertThat(courtDefendantStructure.getCourtCorporateDefendant().getPNCidentifier(), is(caseDefendant.getPncId()));
        assertThat(courtDefendantStructure.getCourtCorporateDefendant().getPresentAtHearing(), is(caseDefendant.getCorporateDefendant().getPresentAtHearing()));
        assertCorporateDefendantAddress(primaryAddress, courtDefendantStructure);

        if(null != courtDefendantStructure.getCourtIndividualDefendant()) {
           assertThat(courtDefendantStructure.getCourtIndividualDefendant().getBailStatus(), is(caseDefendant.getIndividualDefendant().getBailStatus().getCode()));
           assertThat(courtDefendantStructure.getCourtIndividualDefendant().getPresentAtHearing(), is(caseDefendant.getIndividualDefendant().getPresentAtHearing()));
        }

       if(null!= courtDefendantStructure.getOffence()) {
          List<CourtOffenceStructure> structureList = courtDefendantStructure.getOffence();
           for(CourtOffenceStructure courtOffenceStructure : structureList ){
               assertThat(courtOffenceStructure.getConvictingCourt(), is(valueOf(PSA_CODE)));
               assertThat(courtOffenceStructure.getBaseOffenceDetails().getAlcoholRelatedOffence().getAlcoholLevelAmount(), is(caseDefendant.getOffences().get(0).getOffenceFacts().getAlcoholReadingAmount()));
               assertThat(courtOffenceStructure.getBaseOffenceDetails().getAlcoholRelatedOffence().getAlcoholLevelMethod(), is(caseDefendant.getOffences().get(0).getOffenceFacts().getAlcoholReadingMethodCode()));
               assertThat(courtOffenceStructure.getBaseOffenceDetails().getArrestDate(), is(getXmlGregorianCalendar(caseDefendant.getOffences().get(0).getArrestDate())));
               assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceWording(), is(caseDefendant.getOffences().get(0).getWording()));
               assertThat(courtOffenceStructure.getBaseOffenceDetails().getOffenceCode(), is(caseDefendant.getOffences().get(0).getOffenceCode()));

               assertThat(courtOffenceStructure.getConvictionDate(),is(getXmlGregorianCalendar(caseDefendant.getOffences().get(0).getConvictionDate())));
               assertThat(courtOffenceStructure.getFinalDisposalIndicator(), is(caseDefendant.getOffences().get(0).getFinalDisposal()));
               assertThat(courtOffenceStructure.getFinding(), is(caseDefendant.getOffences().get(0).getFinding()));

           }
       }

    }

    @Test
    public void testBuildCourtDefendantStructureWithNoJudicialResultsOnOffenceShouldNotBeSentToSPI () {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);

        CaseDefendant caseDefendant = TestTemplate.buildCaseDefendant();
        caseDefendant.getOffences().add(TestTemplate.buildOffenceDetailsWithJudicialResults(null));
        caseDefendant.getOffences().add(TestTemplate.buildOffenceDetailsWithJudicialResults(Collections.emptyList()));
        Address primaryAddress = caseDefendant.getCorporateDefendant().getAddress();
        final HashMap<String, Object> context = new HashMap<>();

        assertThat(caseDefendant.getOffences().size(), is(3));
        CourtDefendantStructure  courtDefendantStructure =  courtDefendant.buildCourtDefendantStructure(caseDefendant, PSA_CODE, context, getXmlGregorianCalendarFromLocalDate(HEARING_DATE));
        assertNotNull(courtDefendantStructure);
        assertThat(courtDefendantStructure.getProsecutorReference(), is(caseDefendant.getProsecutorReference()));
        assertThat(courtDefendantStructure.getOffence().size(), is(1));
    }

    private void assertCorporateDefendantAddress(final Address primaryAddress, final CourtDefendantStructure courtDefendantStructure) {
        if(null != courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress()) {
            assertThat(courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress().getAddressLine1(), is(primaryAddress.getAddress1()));
            assertThat(courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress().getAddressLine2(), is(primaryAddress.getAddress2()));
            assertThat(courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress().getAddressLine3(), is(primaryAddress.getAddress3()));
            assertThat(courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress().getAddressLine4(), is(primaryAddress.getAddress4()));
            assertThat(courtDefendantStructure.getCourtCorporateDefendant().getAddress().getSimpleAddress().getAddressLine5(), is(primaryAddress.getPostcode()));
        }
    }
}
