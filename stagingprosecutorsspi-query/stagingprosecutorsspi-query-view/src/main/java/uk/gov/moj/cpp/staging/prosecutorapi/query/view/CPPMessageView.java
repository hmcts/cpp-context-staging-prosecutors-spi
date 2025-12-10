package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutorapi.query.view.service.CPPMessageService;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(QUERY_VIEW)
public class CPPMessageView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private CPPMessageService service;

    @Handles("stagingprosecutorsspi.query.cpp-message-view")
    public JsonEnvelope queryCPPMessage(final JsonEnvelope envelope) {
        final String ptiUrn = envelope.payloadAsJsonObject().getString("ptiUrn");

        final List<CPPMessage> cppMessages = service.getCPPMessages(ptiUrn);

        return envelopeFrom(JsonEnvelope.metadataFrom(envelope.metadata())
                .withName("stagingprosecutorsspi.query.cpp.message"), getCppMessagesPayload(cppMessages));
    }

    private JsonObject getCppMessagesPayload(final List<CPPMessage> cppMessages) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        cppMessages.stream().forEach(cppMessage ->
                {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("oiId", cppMessage.getOiId().toString())
                            .add("correlationId", cppMessage.getCorrelationID())
                            .add("policeSystemId", cppMessage.getPoliceSystemId())
                            .add("ptiUrn", cppMessage.getPtiUrn())
                            .add("caseId", cppMessage.getCaseId().toString());
                    if (cppMessage.getDataController() != null) {
                        builder.add("dataController", cppMessage.getDataController());
                    }
                    if (cppMessage.getDataController() != null) {
                        builder.add("organizationUnitID", cppMessage.getOrganizationUnitID());
                    }
                    jsonArrayBuilder.add(builder.build());
                }
        );

        return createObjectBuilder()
                .add("cppMessages", jsonArrayBuilder.build()).build();
    }


}
