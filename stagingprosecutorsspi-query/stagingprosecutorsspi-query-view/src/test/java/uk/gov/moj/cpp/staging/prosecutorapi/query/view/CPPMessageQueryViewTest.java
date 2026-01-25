package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.CPPMessageService;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@ExtendWith(MockitoExtension.class)
public class CPPMessageQueryViewTest {
    final String ptiUrn = "pti urn";
    final JsonEnvelope envelope = envelopeFrom(
            metadataWithRandomUUID("stagingprosecutorsspi.query.cpp.message"),
            createObjectBuilder()
                    .add("ptiUrn", ptiUrn)
                    .build()
    );
    @InjectMocks
    private CPPMessageView view;

    @Mock
    private CPPMessageService service;

    @Test
    public void queriesForCPPMessageUsingRepository() throws IOException {
        final UUID oiId = randomUUID();
        final UUID caseId = randomUUID();

        when(service.getCPPMessages(ptiUrn)).thenReturn(Arrays.asList(getCPPMessageWithDataControllerAndOrgId(oiId, caseId)));

        final JsonEnvelope jsonEnvelope = view.queryCPPMessage(envelope);
        JSONAssert.assertEquals(getExpectedJsonString("expectedCppMessagesPayload.json", caseId, oiId), jsonEnvelope.payload().toString(), JSONCompareMode.STRICT);
    }

    @Test
    public void queriesForCPPMessageWithNullDataControllerAndOrgUsingRepository() throws IOException {
        final UUID oiId = randomUUID();
        final UUID caseId = randomUUID();
        when(service.getCPPMessages(ptiUrn)).thenReturn(Arrays.asList(getCPPMessage(oiId, caseId)));

        final JsonEnvelope jsonEnvelope = view.queryCPPMessage(envelope);
        JSONAssert.assertEquals(getExpectedJsonString("expectedCppMessagesWithoutDataControllerAndOrgIdPayload.json", caseId, oiId), jsonEnvelope.payload().toString(), JSONCompareMode.STRICT);
    }

    private String getExpectedJsonString(String fileName, UUID caseId, UUID oiId) throws IOException {
        return IOUtils.toString(this.getClass().getClassLoader().getResource(fileName))
                .replace("CASE_ID", caseId.toString())
                .replace("OI_ID", oiId.toString());
    }

    private CPPMessage getCPPMessage(UUID oiId, UUID caseId) {
        return new CPPMessage(oiId, ptiUrn, caseId, "police system id", "correlation id");
    }

    private CPPMessage getCPPMessageWithDataControllerAndOrgId(UUID oiId, UUID caseId) {
        CPPMessage cppMessage = new CPPMessage(oiId, ptiUrn, caseId, "police system id", "correlation id");
        cppMessage.setDataController("data controller");
        cppMessage.setOrganizationUnitID("Org unit");
        return cppMessage;
    }
}