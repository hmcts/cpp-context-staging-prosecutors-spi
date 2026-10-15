package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * The same scenarios exist on the J17 copy (main, DeltaSpike repositories, CdiTestRunner). Here the hand-written J25
 * repositories run on the shared test persistence unit with the EntityManager injected directly. Every assertion
 * encodes the J17 result, so a red test is a behaviour change introduced by the upgrade.
 */
class ViewstoreRepositoryParityTest {

    private static final String PERSISTENCE_UNIT = "stagingprosecutors-test-persistence-unit";
    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CPPMessageRepository cppMessageRepository;
    private SpiOutMessageRepository spiOutMessageRepository;
    private EntityManager entityManager;
    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    void setUp() {
        originalDefaultTimeZone = TimeZone.getDefault();
        cppMessageRepository = new CPPMessageRepository();
        spiOutMessageRepository = new SpiOutMessageRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cppMessageRepository);
        hibernateTestEntityManagerProvider.injectEntityManagerInto(spiOutMessageRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
    }

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // SPI-P02: on J17 the framework EntityManagerProducer sets the JVM default time zone to UTC when it creates the
    // EntityManager the repositories use. J25 repositories no longer go through that producer.
    @Test
    void spiP02_shouldSetJvmDefaultTimeZoneToUtcWhenRepositoryIsUsed() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        cppMessageRepository.save(cppMessage());

        assertThat(TimeZone.getDefault().getID(), is("UTC"));
    }

    // SPI-P02: spi_out_message.timestamp is a column without time zone; J17 stores the UTC wall-clock time.
    // findLatestSpiMessageForCaseUrnAndDefendantReference orders by this column.
    // Harness note: hbm2ddl on Hibernate 6+ creates this column WITH time zone, unlike Liquibase (DATETIME) and the
    // J17 harness, so the column is first changed back to TIMESTAMP to match production.
    @Test
    void spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock() {
        entityManager.createNativeQuery("alter table spi_out_message alter column timestamp set data type timestamp").executeUpdate();
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

    // SPI-P03: on J17 DeltaSpike save() persists a new entity and returns the same, now managed, instance.
    // The J25 save() always merges, which returns a managed copy and leaves the argument detached.
    @Test
    void spiP03_saveOfNewEntityShouldReturnTheSameManagedInstance() {
        final CPPMessage cppMessage = cppMessage();

        final CPPMessage saved = cppMessageRepository.save(cppMessage);

        assertThat(saved, sameInstance(cppMessage));
        assertThat(entityManager.contains(cppMessage), is(true));
    }

    private static CPPMessage cppMessage() {
        return new CPPMessage(randomUUID(), "case_urn", randomUUID(), "police_system_id", "correlation_id");
    }
}
