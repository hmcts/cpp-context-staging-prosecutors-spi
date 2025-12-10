package uk.gov.moj.cpp.staging.command.service;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SystemCodes;

import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataQueryServiceTest {

    private static final String REFERENCEDATA_QUERY_SYSTEM_CODES = "referencedata.query.cjs-it-system-codes";
    private static final String REFERENCEDATA_QUERY_PTIURN_TO_OUCODE = "referencedata.query.ptiurn-to-oucode";
    private static final String FIELD_SYSTEM_CODES = "cjsItSystemCodes";
    private static final String FIELD_PTI_URN = "ptiurn";
    private static final String FIELD_OU_CODE = "oucode";

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataQueryService referenceDataService;

    @Test
    public void shouldRetrieveSystemCodesWhenValidSystemCode() {
        final Envelope<JsonObject> mockRefDataEnvelope = buildSystemCodes();

        when(requester.requestAsAdmin(any(), any(Class.class))).thenReturn(mockRefDataEnvelope);

        final List<SystemCodes> refSystemCodes = referenceDataService.retrieveSystemCodes();
        assertThat(refSystemCodes, is(notNullValue()));
        assertThat(refSystemCodes.size(), is(2));
    }

    private Envelope<JsonObject> buildSystemCodes() {
        final JsonArrayBuilder systemCodesArrayBuilder = createArrayBuilder();

        final JsonObject systemCode1 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("seqNum", 10)
                .add("spiInFlag", new Boolean(true))
                .add("systemCode", "00101PoliceCaseSystem")
                .add("systemCodeDescription", "Police : London – Metropolitan")
                .add("validFrom", "2019-04-01")
                .build();

        final JsonObject systemCode2 = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("seqNum", 10)
                .add("spiInFlag", new Boolean(false))
                .add("systemCode", "00301PoliceCaseSystem")
                .add("systemCodeDescription", "Police : London – Metropolitan")
                .add("validFrom", "2019-04-01")
                .build();

        return Envelope.envelopeFrom(
                metadataWithRandomUUID(REFERENCEDATA_QUERY_SYSTEM_CODES),
                createObjectBuilder()
                        .add(FIELD_SYSTEM_CODES, systemCodesArrayBuilder
                                .add(systemCode1)
                                .add(systemCode2))
                        .build());

    }

    private Envelope<JsonObject> buildOuCodeResponse() {
        return Envelope.envelopeFrom(
                metadataWithRandomUUID(REFERENCEDATA_QUERY_PTIURN_TO_OUCODE),
                createObjectBuilder()
                        .add(FIELD_OU_CODE, "000AB")
                        .build());
    }
}