package uk.gov.moj.cpp.staging.prosecutors.persistence.repository;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.SpiOutMessage;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SpiOutMessageRepository extends EntityRepository<SpiOutMessage, UUID> {

    // top operator is natively supported only from version 1.6.2 and we are on version 1.6.1
    @Query(isNative = true, value = "select * from spi_out_message s where s.case_urn = ?1 and s.defendant_reference = ?2 order by timestamp desc limit 1")
    List<SpiOutMessage> findLatestSpiMessageForCaseUrnAndDefendantReference(String caseUrn, String defendantReference);

}


