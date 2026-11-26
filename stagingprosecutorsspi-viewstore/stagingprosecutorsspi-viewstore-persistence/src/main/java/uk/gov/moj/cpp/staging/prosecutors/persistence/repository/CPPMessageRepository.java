package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CPPMessageRepository extends EntityRepository<CPPMessage, UUID> {
    List<CPPMessage> findByPtiUrn(String ptiUrn);
}


