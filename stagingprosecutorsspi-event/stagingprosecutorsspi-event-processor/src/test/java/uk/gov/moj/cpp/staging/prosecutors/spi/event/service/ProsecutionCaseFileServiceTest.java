package uk.gov.moj.cpp.staging.prosecutors.spi.event.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.EventClientTestBase;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileServiceTest {

    public static final String JSON_PROSECUTION_CASE_JSON = "json/prosecutioncasefile.query.case.json";
    public static final String JSON_PROSECUTION_CASE_WITH_ONE_OFFENCE_NO_OFFENCE_LOCATION_JSON = "json/case-single-offence-with-no-offence-location.json";
    public static final String JSON_PROSECUTION_CASE_WITH_MULTI_OFFENCE_OFFENCE_LOCATION_JSON = "json/case-multiple-offence-with-all-offence-has-location.json";
    public static final String JSON_PROSECUTION_CASE_WITH_MULTI_OFFENCE_ONE_OFFENCE_LOCATION_JSON = "json/case-multiple-offence-with-one-offence-has-location.json";
    private static final String PROSECUTION_CASE_QUERY = "prosecutioncasefile.query.case";



    @InjectMocks
    private ProsecutionCaseFileService prosecutionCaseFileService;

    @Mock
    private Requester requester;

    @Test
    public void shouldSetLocationOfOffenceWhenCaseFileHaveOffenceLocationGivenOneOffence() {


        final JsonObject jsonObjectPayload = EventClientTestBase.readJson(JSON_PROSECUTION_CASE_JSON, JsonObject.class);
        final Metadata metadata = EventClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);



        final OffenceDetails offenceDetails = createOffence(fromString("b3947192-2149-4f8a-8342-67620295e5bd"));
        final CaseDefendant defendant = createDefendant(fromString("5ac172a2-3332-4dcc-bd03-20fa0c7957b5"), singletonList(offenceDetails));
        final PublicPoliceResultGenerated policeResult = createPoliceResult(fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"), defendant);


        final Map<String, Object> resultMap = prosecutionCaseFileService.extractOffenceLocation(envelope, requester, fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"));

        assertThat(resultMap.containsKey("b3947192-2149-4f8a-8342-67620295e5bd"), is(true));
        assertThat(resultMap.get("b3947192-2149-4f8a-8342-67620295e5bd").toString(), is("London"));
    }

    @Test
    public void shouldSetLocationOfOffenceWhenCaseFileHaveOffenceLocationGivenMultipleOffence() {


        final JsonObject jsonObjectPayload = EventClientTestBase.readJson(JSON_PROSECUTION_CASE_WITH_MULTI_OFFENCE_OFFENCE_LOCATION_JSON, JsonObject.class);
        final Metadata metadata = EventClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);



        final OffenceDetails offenceDetails = createOffence(fromString("b3947192-2149-4f8a-8342-67620295e5bd"));
        final OffenceDetails offenceDetails1 = createOffence(fromString("2a208dd4-d91d-407b-bb90-bafe2cdcb892"));
        final CaseDefendant defendant = createDefendant(fromString("5ac172a2-3332-4dcc-bd03-20fa0c7957b5"), asList(offenceDetails, offenceDetails1));
        final PublicPoliceResultGenerated policeResult = createPoliceResult(fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"), defendant);


        final Map<String, Object> resultMap = prosecutionCaseFileService.extractOffenceLocation(envelope, requester, fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"));


        assertThat(resultMap.size(), is(2));
        assertThat(resultMap.containsKey("b3947192-2149-4f8a-8342-67620295e5bd"), is(true));
        assertThat(resultMap.get("b3947192-2149-4f8a-8342-67620295e5bd").toString(), is("London"));


        assertThat(resultMap.containsKey("2a208dd4-d91d-407b-bb90-bafe2cdcb892"), is(true));
        assertThat(resultMap.get("2a208dd4-d91d-407b-bb90-bafe2cdcb892").toString(), is("London 2"));

    }

    @Test
    public void shouldSetLocationOfOffenceWhenCaseFileHaveMultipleOffenceLocationAndOffenceLocationIsForOnlyOneOffence() {


        final JsonObject jsonObjectPayload = EventClientTestBase.readJson(JSON_PROSECUTION_CASE_WITH_MULTI_OFFENCE_ONE_OFFENCE_LOCATION_JSON, JsonObject.class);
        final Metadata metadata = EventClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);



        final OffenceDetails offenceDetails = createOffence(fromString("b3947192-2149-4f8a-8342-67620295e5bd"));
        final OffenceDetails offenceDetails1 = createOffence(fromString("2a208dd4-d91d-407b-bb90-bafe2cdcb892"));
        final CaseDefendant defendant = createDefendant(fromString("5ac172a2-3332-4dcc-bd03-20fa0c7957b5"), asList(offenceDetails, offenceDetails1));
        final PublicPoliceResultGenerated policeResult = createPoliceResult(fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"), defendant);


        final Map<String, Object> resultMap = prosecutionCaseFileService.extractOffenceLocation(envelope, requester, fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"));

        assertThat(resultMap.size(), is(1));
        assertThat(resultMap.get("2a208dd4-d91d-407b-bb90-bafe2cdcb892").toString(), is("London 2"));
        assertThat(resultMap.containsKey("b3947192-2149-4f8a-8342-67620295e5bd"), is(false));

    }

    @Test
    public void shouldNotSetLocationOfOffenceWhenCaseFileHaveNoOffenceLocationGivenOneOffence() {


        final JsonObject jsonObjectPayload = EventClientTestBase.readJson(JSON_PROSECUTION_CASE_WITH_ONE_OFFENCE_NO_OFFENCE_LOCATION_JSON, JsonObject.class);
        final Metadata metadata = EventClientTestBase.metadataFor(PROSECUTION_CASE_QUERY, randomUUID().toString());
        final Envelope envelope = Envelope.envelopeFrom(metadata, jsonObjectPayload);
        when(requester.requestAsAdmin(any(), any())).thenReturn(envelope);



        final OffenceDetails offenceDetails = createOffence(fromString("b3947192-2149-4f8a-8342-67620295e5bd"));
        final CaseDefendant defendant = createDefendant(fromString("5ac172a2-3332-4dcc-bd03-20fa0c7957b5"), singletonList(offenceDetails));
        final PublicPoliceResultGenerated policeResult = createPoliceResult(fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"), defendant);


        final Map<String, Object> resultMap = prosecutionCaseFileService.extractOffenceLocation(envelope, requester, fromString("44884cbc-9659-4ae1-a42e-a8c9839df502"));

        assertThat(resultMap.size(), is(0));
    }

    private PublicPoliceResultGenerated createPoliceResult(final UUID caseId, final CaseDefendant caseDefendant) {
        final PublicPoliceResultGenerated publicPoliceResultGenerated = PublicPoliceResultGenerated.publicPoliceResultGenerated();
        publicPoliceResultGenerated.setCaseId(caseId);
        publicPoliceResultGenerated.setDefendant(caseDefendant);
        return publicPoliceResultGenerated;
    }

    private CaseDefendant createDefendant(UUID defendantId, final List<OffenceDetails> offenceDetails) {
       return CaseDefendant.caseDefendant().withDefendantId(defendantId).withOffences(offenceDetails).build();
    }

    private OffenceDetails createOffence(UUID offenceId) {
        return OffenceDetails.offenceDetails().withId(offenceId).build();
    }
}
