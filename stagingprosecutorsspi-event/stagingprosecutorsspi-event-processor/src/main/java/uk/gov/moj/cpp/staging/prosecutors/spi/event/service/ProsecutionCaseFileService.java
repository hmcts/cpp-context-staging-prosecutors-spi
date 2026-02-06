package uk.gov.moj.cpp.staging.prosecutors.spi.event.service;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class ProsecutionCaseFileService {

    private static final String PROSECUTION_CASE_QUERY = "prosecutioncasefile.query.case";

    private JsonObject getProsecutionCase(final Envelope<?> envelope, final Requester requester, final UUID caseId) {


        final JsonObject getCaseDetails = createObjectBuilder().add("caseId", caseId.toString()).build();
        final Envelope<JsonObject> requestEnvelope = Enveloper.envelop(getCaseDetails)
                .withName(PROSECUTION_CASE_QUERY).withMetadataFrom(envelope);
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return response.payload();
    }


    @SuppressWarnings({"squid:S1612"})
    public Map<String, Object> extractOffenceLocation(final Envelope<?> envelope, final Requester requester, final UUID caseId) {

        final HashMap<String, Object> offenceIdWithLocation = new HashMap<>();
        final JsonObject prosecutionCase = getProsecutionCase(envelope, requester, caseId);

        if (nonNull(prosecutionCase)) {
            final JsonArray defendants = prosecutionCase.getJsonArray("defendants");

            IntStream.range(0, defendants.size()).mapToObj(counter -> defendants.getJsonObject(counter)).forEach(defendant -> {
                final JsonArray offences = defendant.getJsonArray("offences");
                IntStream.range(0, offences.size()).mapToObj(offenceCounter -> offences.getJsonObject(offenceCounter)).forEach(offence -> {
                    if (offence.containsKey("offenceLocation")) {
                        final String offenceLocation = offence.getString("offenceLocation");
                        if (isNotBlank(offenceLocation)) {
                            offenceIdWithLocation.put(offence.getString("offenceId"), offenceLocation);
                        }
                    }
                });
            });
        }

        return offenceIdWithLocation;
    }
}
