package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

// needs further investigation - no functional impact - only used in tests
@Ignore("there is discrepancy in behaviour when run from command line on dev laptop and intellij " +
        "and similar discrepancy across verify and validation build")
@RunWith(CdiTestRunner.class)
public class SpiOutMessageRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private SpiOutMessageRepository spiOutMessageRepository;

    // this test is asserting incorrect value as h2 database is returning the data in the opposite order compared to postgres
    @Test
    public void testFindTop1ByCaseUrnAndHearingDateAndDefendantReferenceOrderByTimestampDesc() {
        final String caseUrn = randomAlphanumeric(8);
        final String prosecutorReference = randomAlphanumeric(10);
        final String payload1 = randomAlphanumeric(50);
        final SpiOutMessage oldMessage = createSpiOutMessage(caseUrn, prosecutorReference, ZonedDateTime.now(), payload1);
        List<SpiOutMessage> messageFromDb = spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, prosecutorReference);
        assertThat(messageFromDb, hasSize(1));
        assertThat(messageFromDb.get(0).getPayload(), is(oldMessage.getPayload()));

        final String payload2 = randomAlphanumeric(60);
        final SpiOutMessage newMessage = createSpiOutMessage(caseUrn, prosecutorReference, ZonedDateTime.now().plusMinutes(5), payload2);
        final List<SpiOutMessage> messageFromDbForSecondCall = spiOutMessageRepository.findLatestSpiMessageForCaseUrnAndDefendantReference(caseUrn, prosecutorReference);
        assertThat(messageFromDbForSecondCall, hasSize(1));
        // this needs to assert against newMessage payload
        assertThat(messageFromDbForSecondCall.get(0).getPayload(), is(newMessage.getPayload()));
    }

    private SpiOutMessage createSpiOutMessage(final String caseUrn, final String prosecutorReference, final ZonedDateTime timestamp, final String payload) {
        final SpiOutMessage spiOutMessage = new SpiOutMessage(UUID.randomUUID(), caseUrn, timestamp, prosecutorReference, payload);
        spiOutMessageRepository.save(spiOutMessage);
        return spiOutMessage;
    }
}