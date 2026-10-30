package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CPPRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingprosecutors-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CPPMessageRepository cppMessageRepository;

    @BeforeEach
    public void createRepository() {
        cppMessageRepository = new CPPMessageRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(cppMessageRepository);
    }

    @Test
    public void shouldSaveCppMessage() {

        final UUID oiId = randomUUID();

        final String caseUrn = "case_urn";
        final UUID caseId = randomUUID();
        final String policeSystemId = "police_system_id_xxx";
        final String correlationID = "CorrelationID";
        final CPPMessage cppMessage = new CPPMessage(oiId, caseUrn, caseId, policeSystemId, correlationID);

        cppMessageRepository.save(cppMessage);
        flushAndClear();

        final CPPMessage cppMessageSaved = cppMessageRepository.findBy(oiId);

        assertThat(cppMessageSaved, not(nullValue()));
        assertThat(cppMessageSaved.getCaseId(), is(caseId));
        assertThat(cppMessageSaved.getOiId(), is(oiId));
        assertThat(cppMessageSaved.getCorrelationID(), is(correlationID));
        assertThat(cppMessageSaved.getPoliceSystemId(), is(policeSystemId));
    }

    @Test
    public void shouldReturnNullWhenNoCppMessageExistsForId() {
        assertThat(cppMessageRepository.findBy(randomUUID()), is(nullValue()));
    }

    @Test
    public void shouldFindCppMessagesByPtiUrn() {
        final UUID matchingOiId = randomUUID();
        cppMessageRepository.save(new CPPMessage(matchingOiId, "pti_urn_1", randomUUID(), "police_system_id", "correlation_id_1"));
        cppMessageRepository.save(new CPPMessage(randomUUID(), "pti_urn_2", randomUUID(), "police_system_id", "correlation_id_2"));
        flushAndClear();

        final List<CPPMessage> cppMessages = cppMessageRepository.findByPtiUrn("pti_urn_1");

        assertThat(cppMessages.stream().map(CPPMessage::getOiId).toList(), contains(matchingOiId));
    }

    @Test
    public void shouldReturnEmptyListWhenNoCppMessageExistsForPtiUrn() {
        assertThat(cppMessageRepository.findByPtiUrn("unknown_pti_urn"), is(empty()));
    }

    private void flushAndClear() {
        hibernateTestEntityManagerProvider.getEntityManager().flush();
        hibernateTestEntityManagerProvider.getEntityManager().clear();
    }
}
