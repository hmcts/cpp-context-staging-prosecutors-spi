package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * On J17 the repositories are DeltaSpike {@code EntityRepository} interfaces that use the framework's request-scoped
 * EntityManager (EntityManagerProducer). The J25 upgrade replaces them with hand-written classes using a
 * container-managed {@code @PersistenceContext}. Every assertion below encodes the J17 result.
 *
 * Harness note: under HSQLDialect on H2 a UUID primary key does not round-trip after the persistence context is
 * cleared, so these tests never look an entity up by id after a clear.
 */
@RunWith(CdiTestRunner.class)
public class ViewstoreRepositoryParityTest extends BaseTransactionalJunit4Test {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @Inject
    private CPPMessageRepository cppMessageRepository;

    @Inject
    private SpiOutMessageRepository spiOutMessageRepository;

    @Inject
    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @Before
    public void rememberDefaultTimeZone() {
        originalDefaultTimeZone = TimeZone.getDefault();
    }

    @After
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // SPI-P02: the framework EntityManagerProducer sets the JVM default time zone to UTC when it creates the
    // EntityManager the repositories use. J25 repositories no longer go through that producer.
    @Test
    public void spiP02_shouldSetJvmDefaultTimeZoneToUtcWhenRepositoryIsUsed() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        cppMessageRepository.save(cppMessage());

        assertThat(TimeZone.getDefault().getID(), is("UTC"));
    }

    // SPI-P02: spi_out_message.timestamp is a column without time zone; J17 stores the UTC wall-clock time.
    // findLatestSpiMessageForCaseUrnAndDefendantReference orders by this column.
    @Test
    public void spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock() {
        final String caseUrn = "URN" + randomUUID().toString().substring(0, 8);
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        spiOutMessageRepository.save(new SpiOutMessage(randomUUID(), caseUrn,
                ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, LONDON), "DEF-REF", "payload"));
        entityManager.flush();

        final Object storedTimestamp = entityManager
                .createNativeQuery("select cast(timestamp as varchar(64)) from spi_out_message where case_urn = ?1")
                .setParameter(1, caseUrn)
                .getSingleResult();
        assertThat(storedTimestamp, is("2024-06-01 09:00:00"));
    }

    // SPI-P03: DeltaSpike save() persists a new entity and returns the same, now managed, instance.
    // The J25 save() always merges, which returns a managed copy and leaves the argument detached.
    @Test
    public void spiP03_saveOfNewEntityShouldReturnTheSameManagedInstance() {
        final CPPMessage cppMessage = cppMessage();

        final CPPMessage saved = cppMessageRepository.save(cppMessage);

        assertThat(saved, sameInstance(cppMessage));
        assertThat(entityManager.contains(cppMessage), is(true));
    }

    private static CPPMessage cppMessage() {
        return new CPPMessage(randomUUID(), "case_urn", randomUUID(), "police_system_id", "correlation_id");
    }
}
