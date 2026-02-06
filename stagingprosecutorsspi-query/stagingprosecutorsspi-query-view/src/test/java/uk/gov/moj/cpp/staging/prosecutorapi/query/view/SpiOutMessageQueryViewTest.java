package uk.gov.moj.cpp.staging.prosecutorapi.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.SpiOutMessageRepository;

import java.io.IOException;
import java.time.ZonedDateTime;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SpiOutMessageQueryViewTest {
    private static final String XML_PAYLOAD = "<xml><attr>random payload</attr></xml>";

    final String caseUrn = randomAlphanumeric(8);
    final String defendantProsecutorReference = randomAlphanumeric(15);

    final JsonEnvelope envelope = envelopeFrom(
            metadataWithRandomUUID("stagingprosecutorsspi.query.spi.message"),
            createObjectBuilder()
                    .add("caseUrn", caseUrn)
                    .add("defendantProsecutorReference", defendantProsecutorReference)
                    .build()
    );
    @InjectMocks
    private SpiOutMessageView view;

    @Mock
    private SpiOutMessageRepository repository;

    @Test
    public void queriesForSpiOutMessageUsingRepository() throws IOException {
        when(repository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, defendantProsecutorReference)).thenReturn(newArrayList(getSpiOutMessage()));

        final JsonEnvelope jsonEnvelope = view.querySpiOutMessage(envelope);
        final JsonObject payloadAsJsonObject = jsonEnvelope.payloadAsJsonObject();
        assertThat(payloadAsJsonObject.getString("caseUrn"), is(caseUrn));
        assertThat(payloadAsJsonObject.getString("defendantProsecutorReference"), is(defendantProsecutorReference));
        assertThat(payloadAsJsonObject.getString("payload"), is(XML_PAYLOAD));
    }

    @Test
    public void queriesForSpiOutMessageUsingRepository_NoResultAvailable() throws IOException {
        when(repository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, defendantProsecutorReference)).thenReturn(newArrayList());

        final JsonEnvelope jsonEnvelope = view.querySpiOutMessage(envelope);
        assertTrue(jsonEnvelope.payloadIsNull());
    }

    private SpiOutMessage getSpiOutMessage() {
        return new SpiOutMessage(randomUUID(), caseUrn, ZonedDateTime.now(), defendantProsecutorReference, XML_PAYLOAD);
    }
}