package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SpiOutMessageRepository {

    @PersistenceContext(unitName = "stagingprosecutors")
    EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<SpiOutMessage> findLatestSpiMessageForCaseUrnAndDefendantReference(final String caseUrn, final String defendantReference) {
        return entityManager.createNativeQuery(
                        "select * from spi_out_message s where s.case_urn = ?1 and s.defendant_reference = ?2 order by timestamp desc limit 1",
                        SpiOutMessage.class)
                .setParameter(1, caseUrn)
                .setParameter(2, defendantReference)
                .getResultList();
    }

    public SpiOutMessage save(final SpiOutMessage spiOutMessage) {
        return entityManager.merge(spiOutMessage);
    }
}
