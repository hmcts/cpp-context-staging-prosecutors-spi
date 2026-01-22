package uk.gov.moj.cpp.staging.command.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SystemCodes;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.service.ReferenceDataService;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataQueryService implements ReferenceDataService {

    private static final String REFERENCEDATA_QUERY_SYSTEM_CODES = "referencedata.query.cjs-it-system-codes";
    private static final String FIELD_SYSTEM_CODES = "cjsItSystemCodes";
    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataQueryService.class);

    @Inject
    @ServiceComponent(COMMAND_HANDLER)
    private Requester requester;

    @Override
    public List<SystemCodes> retrieveSystemCodes() {
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(REFERENCEDATA_QUERY_SYSTEM_CODES);

        final Envelope<JsonObject> responseEnvelope = requester.requestAsAdmin(envelopeFrom(metadataBuilder, createObjectBuilder()), JsonObject.class);
        final JsonArray response = responseEnvelope.payload().getJsonArray(FIELD_SYSTEM_CODES);

        List<SystemCodes> systemCodesReferenceData = null;
        if (null != response) {
            systemCodesReferenceData = response.stream().map(asSystemCodesRefData()).collect(Collectors.toList());
        }

        return systemCodesReferenceData;
    }

    private static Function<JsonValue, SystemCodes> asSystemCodesRefData() {
        return jsonValue -> {
            try {
                return objectMapper.readValue(jsonValue.toString(), SystemCodes.class);
            } catch (IOException ex) {
                LOGGER.error("Unable to unmarshal SystemCodesReferenceData. Exception :{}", ex);
                return null;
            }
        };
    }
}
