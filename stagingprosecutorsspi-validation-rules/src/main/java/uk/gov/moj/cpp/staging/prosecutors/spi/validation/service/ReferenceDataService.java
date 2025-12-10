package uk.gov.moj.cpp.staging.prosecutors.spi.validation.service;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SystemCodes;

import java.util.List;

public interface ReferenceDataService {

    List<SystemCodes> retrieveSystemCodes();

}
