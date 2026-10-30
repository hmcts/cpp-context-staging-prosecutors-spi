package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SpiOutMessageRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingprosecutors-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SpiOutMessageRepository spiOutMessageRepository;

    @BeforeEach
    public void createRepository() {
        spiOutMessageRepository = new SpiOutMessageRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(spiOutMessageRepository);
    }

    @Test
    public void testFindTop1ByCaseUrnAndHearingDateAndDefendantReferenceOrderByTimestampDesc() {
        final String caseUrn = randomAlphanumeric(8);
        final String prosecutorReference = randomAlphanumeric(10);
        final String payload1 = randomAlphanumeric(50);
        final SpiOutMessage oldMessage = createSpiOutMessage(caseUrn, prosecutorReference, ZonedDateTime.now(), payload1);
        final List<SpiOutMessage> messageFromDb = spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, prosecutorReference);
        assertThat(messageFromDb, hasSize(1));
        assertThat(messageFromDb.get(0).getPayload(), is(oldMessage.getPayload()));

        final String payload2 = randomAlphanumeric(60);
        final SpiOutMessage newMessage = createSpiOutMessage(caseUrn, prosecutorReference, ZonedDateTime.now().plusMinutes(5), payload2);
        final List<SpiOutMessage> messageFromDbForSecondCall = spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, prosecutorReference);
        assertThat(messageFromDbForSecondCall, hasSize(1));
        assertThat(messageFromDbForSecondCall.get(0).getPayload(), is(newMessage.getPayload()));
    }

    @Test
    public void shouldReturnEmptyListWhenNoSpiOutMessageMatches() {
        createSpiOutMessage(randomAlphanumeric(8), randomAlphanumeric(10), ZonedDateTime.now(), randomAlphanumeric(50));

        assertThat(spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(randomAlphanumeric(8), randomAlphanumeric(10)), is(empty()));
    }

    private SpiOutMessage createSpiOutMessage(final String caseUrn, final String prosecutorReference, final ZonedDateTime timestamp, final String payload) {
        final SpiOutMessage spiOutMessage = new SpiOutMessage(UUID.randomUUID(), caseUrn, timestamp, prosecutorReference, payload);
        spiOutMessageRepository.save(spiOutMessage);
        hibernateTestEntityManagerProvider.getEntityManager().flush();
        hibernateTestEntityManagerProvider.getEntityManager().clear();
        return spiOutMessage;
    }
}
