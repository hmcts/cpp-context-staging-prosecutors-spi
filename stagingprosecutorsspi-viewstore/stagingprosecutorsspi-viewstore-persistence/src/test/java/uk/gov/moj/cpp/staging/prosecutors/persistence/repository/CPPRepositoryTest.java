package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class CPPRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private CPPMessageRepository cppMessageRepository;

    @Test
    public void shouldSaveCppMessage() {

        final UUID oiId = randomUUID();

        final String caseUrn = "case_urn";
        final UUID caseId = randomUUID();
        final String policeSystemId = "police_system_id_xxx";
        final String correlationID = "CorrelationID";
        final CPPMessage cppMessage = new CPPMessage(oiId, caseUrn, caseId, policeSystemId, correlationID);

        cppMessageRepository.save(cppMessage);

        final CPPMessage cppMessageSaved = cppMessageRepository.findBy(oiId);

        assertThat(cppMessageSaved, not(nullValue()));
        assertThat(cppMessageSaved.getCaseId(), is(caseId));
        assertThat(cppMessageSaved.getOiId(), is(oiId));
        assertThat(cppMessageSaved.getCorrelationID(), is(correlationID));
        assertThat(cppMessageSaved.getPoliceSystemId(), is(policeSystemId));
    }
}
