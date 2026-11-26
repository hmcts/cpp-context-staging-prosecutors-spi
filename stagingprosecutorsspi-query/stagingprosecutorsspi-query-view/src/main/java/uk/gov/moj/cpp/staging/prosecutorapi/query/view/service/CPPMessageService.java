package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;

import java.util.List;

import javax.inject.Inject;

public class CPPMessageService {
    @Inject
    private CPPMessageRepository repository;

    public List<CPPMessage> getCPPMessages(final String ptiUrn) {
        return repository.findByPtiUrn(ptiUrn);
    }
}
