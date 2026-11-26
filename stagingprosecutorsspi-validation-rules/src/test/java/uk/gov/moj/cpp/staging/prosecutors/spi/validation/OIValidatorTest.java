package uk.gov.moj.cpp.staging.prosecutors.spi.validation;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.DISALLOWED_DATA_STREAM_TYPE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_CORRELATION_ID_CODE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_DATA_CONTROLLER;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ORGANIZATIONAL_UNIT_ID;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_PREVIOUSLY_SENT_REQUEST;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ROUTE_DESTINATION_SYSTEM;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ROUTE_SOURCE_SYSTEM;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_SYSTEM_ID_CODE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.SAME_SOURCE_AND_DESTINATION_SYSTEM;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.UNUSABLE_DATA_STREAM;

import uk.gov.cjse.schemas.common.businessentities.DataStreamStructure;
import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SystemCodes;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.CorrelationIdOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.DataStreamOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.DataStreamTypeOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.InvalidDataControllerOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.InvalidOrganizationalUnitIdOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.OIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.PreviouslySentOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.RouteDestinationSystemOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.RouteSourceSystemOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.SameSourceAndDestinationOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.SystemIdOIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OIValidatorTest {

    private static final String INVALID_SOURCE_SYSTEM = "InvalidSourceSystem";
    private static final String INVALID_DESTINATION_SYSTEM = "InvalidDestinationSystem";
    private static final String INVALID_SYSTEM_ID_GT_50 = "SYSIDSYSIDSYSIDSYSIDSYSIDSYSIDSYSIDSYSIDSYSIDSYSIDSYSID";
    private static final String INVALID_CORRELATION_ID_VALUE = "CORRELATION ID";
    private static final String INVALID_DATA_CONTROLLER_GT_50 = "DATACONTROLLERDATACONTROLLERDATACONTROLLERDATACONTROLLER";
    private static final String INVALID_ORGANISATION_VALUE = "ORG";
    private static final String VALID_SOURCE_SYSTEM_CODE = "00501PoliceCaseSystem";
    private static final String INVALID_SOURCE_SYSTEM_CODE = "00301PoliceCaseSystem";
    @InjectMocks
    private final RouteSourceSystemOIValidationRule routeSourceSystemOIValidationRule = new RouteSourceSystemOIValidationRule();
    private final RouteDestinationSystemOIValidationRule routeDestinationSystemOIValidationRule = new RouteDestinationSystemOIValidationRule();
    private final DataStreamTypeOIValidationRule dataStreamTypeOIValidationRule = new DataStreamTypeOIValidationRule();
    private final SystemIdOIValidationRule systemIdOIValidationRule = new SystemIdOIValidationRule();
    private final InvalidOrganizationalUnitIdOIValidationRule invalidOrganizationalUnitIdOIValidationRule = new InvalidOrganizationalUnitIdOIValidationRule();
    private final CorrelationIdOIValidationRule correlationIdOIValidationRule = new CorrelationIdOIValidationRule();
    private final InvalidDataControllerOIValidationRule invalidDataControllerOIValidationRule = new InvalidDataControllerOIValidationRule();
    private final PreviouslySentOIValidationRule previouslySentOIValidationRule = new PreviouslySentOIValidationRule();
    private final DataStreamOIValidationRule dataStreamOIValidationRule = new DataStreamOIValidationRule();
    @Mock
    private ReferenceDataService referenceDataService;
    private SameSourceAndDestinationOIValidationRule sameSourceAndDestinationOIValidationRule = new SameSourceAndDestinationOIValidationRule();
    @InjectMocks
    private OIValidator oiValidator;
    @Mock
    private Instance<OIValidationRule> validatorRules;
    @Mock
    private RouteDataRequestType routeDataRequestType;
    @Mock
    private RouteDataRequestType.Routes routes;
    @Mock
    private RouteDataStreamType.RouteSourceSystem routeSourceSystem;
    @Mock
    private RouteDataStreamType.RouteDestinationSystem routeDestinationSystem;
    @Mock
    private DataStreamStructure dataStreamStructure;
    @Mock
    private DataStreamStructure.DataStreamType dataStreamType;
    @Mock
    private OIInputVO oiInputVO;

    @BeforeEach
    public void setup() throws Exception {
        final String payload = readFileToString(new File(this.getClass().getClassLoader().getResource("IndividualDefendantAllfields.xml").getFile()));
        when(routeDataRequestType.getRoutes()).thenReturn(routes);
        when(routeDataRequestType.getDataStream()).thenReturn(dataStreamStructure);
        when(dataStreamStructure.getDataStreamContent()).thenReturn(payload);
        when(dataStreamStructure.getDataStreamType()).thenReturn(dataStreamType);

        final List<OIValidationRule> validationRules =
                asList(routeSourceSystemOIValidationRule, routeDestinationSystemOIValidationRule, dataStreamTypeOIValidationRule,
                        systemIdOIValidationRule, invalidOrganizationalUnitIdOIValidationRule, correlationIdOIValidationRule, invalidDataControllerOIValidationRule, previouslySentOIValidationRule, sameSourceAndDestinationOIValidationRule, dataStreamOIValidationRule);

        when(validatorRules.spliterator()).thenReturn(validationRules.spliterator());
    }

    private List<SystemCodes> mockValidSystemCodes(final String sourceSystemCode, final boolean spiInFlag) {
        List<SystemCodes> systemCodes = new ArrayList<>();
        SystemCodes systemCode = SystemCodes.systemCodes()
                .withSystemCode(sourceSystemCode)
                .withSpiInFlag(spiInFlag)
                .build();

        systemCodes.add(systemCode);
        return systemCodes;
    }

    @Test
    public void shouldRunAllRulesWithInvalidValuesAndReturnAllErrors() {
        setUpDataForOIValidator(new InputOIValidator(INVALID_SYSTEM_ID_GT_50, INVALID_ORGANISATION_VALUE, INVALID_DATA_CONTROLLER_GT_50, INVALID_CORRELATION_ID_VALUE, INVALID_SOURCE_SYSTEM, INVALID_DESTINATION_SYSTEM, "InvalidStream"));
        when(dataStreamStructure.getDataStreamContent()).thenReturn("invalidPayload");

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_SOURCE_SYSTEM, INVALID_ROUTE_DESTINATION_SYSTEM, DISALLOWED_DATA_STREAM_TYPE,
                INVALID_SYSTEM_ID_CODE, INVALID_ORGANIZATIONAL_UNIT_ID, INVALID_CORRELATION_ID_CODE, INVALID_DATA_CONTROLLER, UNUSABLE_DATA_STREAM));


        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void shouldRunAllRulesWithInvalidValuesAndReturnAllErrorsWithEmptyDataController() {
        setUpDataForOIValidator(new InputOIValidator(INVALID_SYSTEM_ID_GT_50, INVALID_ORGANISATION_VALUE, "", INVALID_CORRELATION_ID_VALUE, INVALID_SOURCE_SYSTEM, INVALID_DESTINATION_SYSTEM, "InvalidStream"));
        when(dataStreamStructure.getDataStreamContent()).thenReturn("invalidPayload");

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_SOURCE_SYSTEM, INVALID_ROUTE_DESTINATION_SYSTEM, DISALLOWED_DATA_STREAM_TYPE,
                INVALID_SYSTEM_ID_CODE, INVALID_ORGANIZATIONAL_UNIT_ID, INVALID_CORRELATION_ID_CODE, INVALID_DATA_CONTROLLER, UNUSABLE_DATA_STREAM));


        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }


    @Test
    public void ShouldRunAllRulesWIthAllValidValues() throws Exception {
        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "00000000", "richard.hanley", "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "Z00CJSE", "SPINewCase"));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);
        final List<ValidationError> expectedValidationErrors = new ArrayList();

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));
    }

    @Test
    public void ShouldRunAllRulesWIthAllValidValuesWithNullValues() throws Exception {
        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", null, null, "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "Z00CJSE", "SPINewCase"));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);
        final List<ValidationError> expectedValidationErrors = new ArrayList();

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));
    }

    @Test
    public void ShouldRunAllRulesWIthAllInValidLength() throws Exception {
        setUpDataForOIValidator(new InputOIValidator("", "", "", "", "", "", ""));
        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_SOURCE_SYSTEM, INVALID_ROUTE_DESTINATION_SYSTEM, DISALLOWED_DATA_STREAM_TYPE,
                INVALID_SYSTEM_ID_CODE, INVALID_CORRELATION_ID_CODE));

        assertTrue(oiValidationErrors.stream().flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList()).size() > 0);

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void ShouldReturnErrorWhenSourceIsCorrectAndSourceAndDestinationAreSame() throws Exception {
        sameSourceAndDestinationOIValidationRule = new SameSourceAndDestinationOIValidationRule();

        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "0000000", "richard.hanley", "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "00501PoliceCaseSystem", "SPINewCase"));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        assertTrue(oiValidationErrors.stream().flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList()).size() > 0);

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_DESTINATION_SYSTEM, SAME_SOURCE_AND_DESTINATION_SYSTEM));
        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void ShouldReturnErrorWhenSourceIsWrongAndSourceAndDestinationAreSame() throws Exception {

        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "0000000", "richard.hanley", "21_05_2018_103408_00000030657", INVALID_SOURCE_SYSTEM_CODE, "00301PoliceCaseSystem", "SPINewCase"));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        assertTrue(oiValidationErrors.stream().flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList()).size() > 0);

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_SOURCE_SYSTEM, SAME_SOURCE_AND_DESTINATION_SYSTEM));
        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void ShouldReturnErrorWhenSourceIsCorrectButSPIFlagIsSetFalse() throws Exception {

        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "0000000", "richard.hanley", "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "00301PoliceCaseSystem", "SPINewCase"));
        when(referenceDataService.retrieveSystemCodes()).thenReturn(mockValidSystemCodes(VALID_SOURCE_SYSTEM_CODE, false));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);

        assertTrue(oiValidationErrors.stream().flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList()).size() > 0);

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_ROUTE_SOURCE_SYSTEM));
        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void ShouldReturnErrorWhenCorrelationIdAndSystemIdIsEqualForOiInputVOAndRouteDataRequestType() throws Exception {
        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "0000000", "richard.hanley", "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "Z00CJSE", "SPINewCase"));

        setRouteDataAndOiInputVOCorrelationIdAndSystemId();

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);
        assertTrue(oiValidationErrors.stream().flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty()).collect(Collectors.toList()).size() > 0);

        final List<ValidationError> expectedValidationErrors = new ArrayList<>(asList(INVALID_PREVIOUSLY_SENT_REQUEST));
        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));

    }

    @Test
    public void ShouldGetErrorInvalidOrganisationUnit() throws Exception {
        setUpDataForOIValidator(new InputOIValidator("00501PoliceCaseSystem", "000000000", "richard.hanley", "21_05_2018_103408_00000030657", VALID_SOURCE_SYSTEM_CODE, "Z00CJSE", "SPINewCase"));

        final List<Optional<ValidationError>> oiValidationErrors = oiValidator.validate(routeDataRequestType, oiInputVO);
        final List<ValidationError> expectedValidationErrors = new ArrayList(asList(INVALID_ORGANIZATIONAL_UNIT_ID));

        assertTrue(oiValidationErrors.stream().filter(s -> s.isPresent()).map(s -> s.get())
                .collect(Collectors.toList()).containsAll(expectedValidationErrors));
    }

    private void setRouteDataAndOiInputVOCorrelationIdAndSystemId() {
        final String correlation_id = UUID.randomUUID().toString();
        routeDataRequestType.getRequestFromSystem().setCorrelationID(correlation_id);
        when(oiInputVO.getCorrelationId()).thenReturn(correlation_id);

        final String system_id = UUID.randomUUID().toString();
        routeDataRequestType.getRequestFromSystem().getSystemID().setValue(system_id);
        when(oiInputVO.getSystemId()).thenReturn(system_id);
    }

    private SystemDetailsStructure getMockSystemDetailsStructure(final String systemId, final String organisationUnit, final String dataControllerValue, final String correlationId) {
        final SystemDetailsStructure systemDetailsStructure = new SystemDetailsStructure();
        final SystemDetailsStructure.SystemID systemID = new SystemDetailsStructure.SystemID();
        systemID.setValue(systemId);
        final SystemDetailsStructure.OrganizationalUnitID organizationalUnitID = new SystemDetailsStructure.OrganizationalUnitID();
        organizationalUnitID.setValue(organisationUnit);
        final SystemDetailsStructure.DataController dataController = new SystemDetailsStructure.DataController();
        dataController.setValue(dataControllerValue);

        systemDetailsStructure.setSystemID(systemID);
        systemDetailsStructure.setOrganizationalUnitID(isNull(organisationUnit) ? null : organizationalUnitID);
        systemDetailsStructure.setCorrelationID(correlationId);
        systemDetailsStructure.setDataController(isNull(dataControllerValue) ? null : dataController);
        return systemDetailsStructure;
    }

    private List<RouteDataStreamType> getMockRouteDataStreamType(final String sourceSystem, final String destinationSystem) {
        final List<RouteDataStreamType> routeDataStreamTypes = new ArrayList<>();

        final RouteDataStreamType routeDataStreamType = new RouteDataStreamType();

        final RouteDataStreamType.RouteSourceSystem routeSourceSystem = new RouteDataStreamType.RouteSourceSystem();
        routeSourceSystem.setValue(sourceSystem);
        routeDataStreamType.setRouteSourceSystem(routeSourceSystem);

        final RouteDataStreamType.RouteDestinationSystem routeDestinationSystem = new RouteDataStreamType.RouteDestinationSystem();
        routeDestinationSystem.setValue(destinationSystem);
        routeDataStreamType.setRouteDestinationSystem(routeDestinationSystem);

        routeDataStreamTypes.add(routeDataStreamType);
        return routeDataStreamTypes;
    }


    private void setUpDataForOIValidator(final InputOIValidator inputOIValidator) {
        when(routes.getRoute()).thenReturn(getMockRouteDataStreamType(inputOIValidator.getSourceSystem(), inputOIValidator.getDestinationSystem()));
        when(routeDataRequestType.getRequestFromSystem()).thenReturn(getMockSystemDetailsStructure(inputOIValidator.getSystemId(), inputOIValidator.getOrganizationUnitId(), inputOIValidator.getDataController(), inputOIValidator.getCorrelationId()));
        when(dataStreamType.getValue()).thenReturn(inputOIValidator.getDataStreamType());
    }

    private class InputOIValidator {
        private final String systemId;
        private final String organizationUnitId;
        private final String dataController;
        private final String correlationId;
        private final String sourceSystem;
        private final String destinationSystem;
        private final String dataStreamType;

        public InputOIValidator(final String systemId, final String organizationUnitId, final String dataController, final String correlationId, final String sourceSystem, final String destinationSystem, final String dataStreamType) {
            this.systemId = systemId;
            this.organizationUnitId = organizationUnitId;
            this.dataController = dataController;
            this.correlationId = correlationId;
            this.sourceSystem = sourceSystem;
            this.destinationSystem = destinationSystem;
            this.dataStreamType = dataStreamType;
        }

        public String getSystemId() {
            return systemId;
        }

        public String getOrganizationUnitId() {
            return organizationUnitId;
        }

        public String getDataController() {
            return dataController;
        }

        public String getCorrelationId() {
            return correlationId;
        }

        public String getSourceSystem() {
            return sourceSystem;
        }

        public String getDestinationSystem() {
            return destinationSystem;
        }

        public String getDataStreamType() {
            return dataStreamType;
        }
    }


}


