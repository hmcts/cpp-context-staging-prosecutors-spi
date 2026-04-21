package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

public class ReferenceDataQueryService {
    private static final String REFERENCEDATA_QUERY_PLEA_TYPES = "referencedata.query.plea-types";
    private static final String FIELD_PLEA_STATUS_TYPES = "pleaStatusTypes";
    private static final String FIELD_PLEA_STATUS_CODE = "pleaStatusCode";
    private static final String FIELD_PLEA_VALUE = "pleaValue";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    public Optional<Integer> retrievePleaStatusCode(final String pleaValue) {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_PLEA_TYPES);

        final Envelope<JsonObject> pleaTypes = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder()), JsonObject.class);
        final JsonArray pleaStatusTypes = pleaTypes.payload().getJsonArray(FIELD_PLEA_STATUS_TYPES);

        return pleaStatusTypes.stream()
                .filter(jsonValue -> pleaValue.equals(((JsonObject) jsonValue).getString(FIELD_PLEA_VALUE)))
                .map(jsonValue -> Integer.parseInt(((JsonObject) jsonValue).getString(FIELD_PLEA_STATUS_CODE)))
                .findFirst();
    }

}
