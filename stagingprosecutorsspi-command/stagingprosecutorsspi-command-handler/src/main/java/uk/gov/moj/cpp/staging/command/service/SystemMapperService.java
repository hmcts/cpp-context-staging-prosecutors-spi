package uk.gov.moj.cpp.staging.command.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.containsWhitespace;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

public class SystemMapperService {


    public static final String SPI_URN = "SPI-URN";
    public static final String CASE_ID = "CASE-ID";
    public static final String SPI_RESULT_ID = "SPI-RESULT-ID";
    public static final String REQUEST_MDI_ID = "REQUEST-MDI-ID";
    public static final String REQUEST_ID = "REQUEST-ID";
    public static final String REQUEST_OI_ID = "REQUEST-OI-ID";
    public static final String COORRELATION_SYSTEM_ID = "COORRELATION_SYSTEM_ID";
    public static final String CONTEXT_SYSTEM_USER_ID_IS_NOT_PRESENT = "Context System User Id is not present";
    private static final String SOURCE_TYPE = "OU_URN";
    private static final String TARGET_TYPE = "CASE_FILE_ID";
    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    public UUID getCaseIdForPtiURNAndOriginatingOrganisation(final String inputString) {
        return getMappedUUIDForInputString(inputString, SOURCE_TYPE, TARGET_TYPE);
    }

    public UUID getSpiResultIdForURN(final String ptiUrn) {
        return getMappedUUIDForInputString(ptiUrn, SPI_URN, SPI_RESULT_ID);
    }

    public UUID getMappedUUIDForRequestId(final String requestString) {
        return getMappedUUIDForInputString(requestString, REQUEST_MDI_ID, REQUEST_ID);
    }


    public UUID getOiIdForCorrelationAndSystemId(String correlationId, String systemId) {
        correlationId = containsWhitespace(correlationId) || isEmpty(correlationId) ? "INVALID_CORRELATION_ID" : correlationId;
        systemId = isEmpty(systemId) ? "INVALID_SYSTEM_ID" : systemId;
        final String correlationAndSystemId = correlationId.concat(systemId);
        return getOperationalInterfaceIdForCorrelationAndSystemId(correlationAndSystemId);
    }

    public UUID getOperationalInterfaceIdForCorrelationAndSystemId(final String correlationAndSystemId) {
        return getMappedUUIDForInputString(correlationAndSystemId, COORRELATION_SYSTEM_ID, REQUEST_OI_ID);
    }

    private UUID getMappedUUIDForInputString(final String inputString, final String sourceType, final String targetType) {
        final Optional<SystemIdMapping> systemIdMappingOption = getSystemIdMappingForInputString(inputString, sourceType, targetType);
        if (systemIdMappingOption.isPresent()) {
            return systemIdMappingOption.map(SystemIdMapping::getTargetId).orElseThrow(() -> new IllegalStateException(format("Unable to creating mapping for input String %s to a uuid", inputString)));
        } else {

            return attemptAddMappingForInputStr(randomUUID(), inputString, sourceType, targetType).orElseThrow(() -> new IllegalStateException(format("Unable to creating mapping for input String %s to a uuid", inputString)));
        }
    }

    private Optional<SystemIdMapping> getSystemIdMappingForInputString(final String inputString, final String sourceType, final String targetType) {
        return systemIdMapperClient.findBy(inputString, sourceType, targetType, systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException(CONTEXT_SYSTEM_USER_ID_IS_NOT_PRESENT)));
    }

    private Optional<UUID> attemptAddMappingForInputStr(final UUID newOutputUUID, final String urn, final String sourceType, final String targetType) {
        final SystemIdMap systemIdMap = new SystemIdMap(urn, sourceType, newOutputUUID, targetType);
        if (systemIdMapperClient.add(systemIdMap, systemUserProvider.getContextSystemUserId().orElseThrow(() -> new IllegalStateException(CONTEXT_SYSTEM_USER_ID_IS_NOT_PRESENT))).isSuccess()) {
            return Optional.of(newOutputUUID);
        }

        return Optional.empty();
    }

}
