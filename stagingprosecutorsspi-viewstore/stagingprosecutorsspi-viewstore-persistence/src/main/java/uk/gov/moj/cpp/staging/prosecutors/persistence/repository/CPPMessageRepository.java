package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CPPMessageRepository {

    @PersistenceContext(unitName = "stagingprosecutors")
    EntityManager entityManager;

    public CPPMessage findBy(final UUID oiId) {
        return entityManager.find(CPPMessage.class, oiId);
    }

    public List<CPPMessage> findByPtiUrn(final String ptiUrn) {
        return entityManager.createQuery("SELECT c FROM CPPMessage c WHERE c.ptiUrn = :ptiUrn", CPPMessage.class)
                .setParameter("ptiUrn", ptiUrn)
                .getResultList();
    }

    public CPPMessage save(final CPPMessage cppMessage) {
        return entityManager.merge(cppMessage);
    }
}
