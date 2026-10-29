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
import static org.hamcrest.Matchers.samePropertyValuesAs;

/**
 * J17/J25 parity characterisation (J17 golden master) for the viewstore repositories.
 *
 * The same scenarios exist on the J17 copy (main, DeltaSpike repositories, CdiTestRunner). Here the hand-written J25
 * repositories run on the shared test persistence unit with the EntityManager injected directly. Every assertion
 * encodes the J17 result, so a red test is a behaviour change introduced by the upgrade. Where J25 differs by design
 * (the JVM default time zone, the identity returned by save()), the test pins the accepted J25 behaviour and says why.
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

    // SPI-P02: on J17 the framework EntityManagerProducer set the JVM default time zone to UTC when it created the
    // EntityManager the repositories used. J25 repositories no longer go through that producer, so using them leaves
    // the JVM default time zone unchanged and the server must run in UTC itself.
    @Test
    void spiP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));

        cppMessageRepository.save(cppMessage());

        assertThat(TimeZone.getDefault().getID(), is("Europe/London"));
    }

    // SPI-P02: spi_out_message.timestamp is a column without time zone; J17 stores the UTC wall-clock time.
    // findLatestSpiMessageForCaseUrnAndDefendantReference orders by this column. J17 got the UTC zone from the
    // EntityManagerProducer (see above); on J25 the server runs in UTC, so the database session zone (taken from the
    // JVM default when the connection opens) is UTC. H2 fixes its session zone at connect, so the test sets it directly.
    // Harness note: hbm2ddl on Hibernate 6+ creates this column WITH time zone, unlike Liquibase (DATETIME) and the
    // J17 harness, so the column is first changed back to TIMESTAMP to match production.
    @Test
    void spiP02_shouldStoreSpiOutMessageTimestampAsUtcWallClock() {
        entityManager.createNativeQuery("alter table spi_out_message alter column timestamp set data type timestamp").executeUpdate();
        final String caseUrn = "URN" + randomUUID().toString().substring(0, 8);
        entityManager.createNativeQuery("set time zone 'UTC'").executeUpdate();

        spiOutMessageRepository.save(new SpiOutMessage(randomUUID(), caseUrn,
                ZonedDateTime.of(2024, 6, 1, 10, 0, 0, 0, LONDON), "DEF-REF", "payload"));
        entityManager.flush();

        final Object storedTimestamp = entityManager
                .createNativeQuery("select cast(timestamp as varchar(64)) from spi_out_message where case_urn = ?1")
                .setParameter(1, caseUrn)
                .getSingleResult();
        assertThat(storedTimestamp, is("2024-06-01 09:00:00"));
    }

    // SPI-P03: save() of a new entity returns a managed entity with the same field values. DeltaSpike (J17) returned
    // the same instance; the J25 merge() returns a managed copy. No caller relies on the identity.
    @Test
    void spiP03_saveOfNewEntityShouldReturnAManagedEntityWithTheSameFields() {
        final CPPMessage cppMessage = cppMessage();

        final CPPMessage saved = cppMessageRepository.save(cppMessage);

        assertThat(saved, samePropertyValuesAs(cppMessage));
        assertThat(entityManager.contains(saved), is(true));
    }

    private static CPPMessage cppMessage() {
        return new CPPMessage(randomUUID(), "case_urn", randomUUID(), "police_system_id", "correlation_id");
    }
}
