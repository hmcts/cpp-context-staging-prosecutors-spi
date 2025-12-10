package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataQueryServiceTest {
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_PLEA_STATUS_CODE = "pleaStatusCode";
    private static final String FIELD_PLEA_VALUE = "pleaValue";

    private static final String PLEA_VALUE_1 = "pleaValue1";
    private static final String PLEA_VALUE_2 = "pleaValue2";
    private static final Integer PLEA_STATUS_CODE_1 = 3;
    private static final Integer PLEA_STATUS_CODE_2 = 5;


    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataQueryService referenceDataService;


    @Test
    public void shouldRetrievePleaStatusCode() {
        final Envelope envelope = envelopeFrom(metadataBuilder()
                        .withId(UUID.randomUUID())
                        .withName("name")
                        .build(),
                buildPleaStatusTypesPayload());

        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(envelope);
        final Optional<Integer> actual = referenceDataService.retrievePleaStatusCode(PLEA_VALUE_1);
        assertThat(actual.isPresent(), is(true));
        assertThat(actual.get(), is(PLEA_STATUS_CODE_1));
    }

    private JsonObject buildPleaStatusTypesPayload() {
        return createObjectBuilder().add(FIELD_PLEA_STATUS_TYPES, createArrayBuilder()
                .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_1).add(FIELD_PLEA_STATUS_CODE, String.valueOf(PLEA_STATUS_CODE_1)))
                .add(createObjectBuilder().add(FIELD_PLEA_VALUE, PLEA_VALUE_2).add(FIELD_PLEA_STATUS_CODE, String.valueOf(PLEA_STATUS_CODE_2))))
                .build();
    }
}
