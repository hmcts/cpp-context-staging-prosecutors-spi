package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OiRequestMessageReceivedListenerTest {

    private static final UUID FIELD_OID = UUID.randomUUID();
    private static final String FIELD_DESTINATION_SYSTEM = "Z00CJSE";
    private static final String FIELD_SOURCE_SYSTEM = "00501PoliceCaseSystem";
    private static final String FIELD_DATA_CONTROLLER = "dataController";
    private static final String FIELD_ORGANISATION_UNIT_ID = "orgUnit";

    @Mock
    private Envelope<OiRequestMessageReceived> oiRequestMessageReceivedEnvelope;
    @Mock
    private CPPMessageRepository cppMessageRepository;

    @Mock
    private RouteDataRequestType routeDataRequestType;

    @Mock
    private OiRequestMessageReceived oiRequestMessageReceived;

    @InjectMocks
    private OiRequestMessageReceivedListener oiRequestMessageReceivedListener;

    @Test
    public void oiRequestMessageReceived() {
        when(oiRequestMessageReceivedEnvelope.payload()).thenReturn(oiRequestMessageReceived);
        when(oiRequestMessageReceived.getOiId()).thenReturn(FIELD_OID);
        when(oiRequestMessageReceived.getRouteDataRequestType()).thenReturn(routeDataRequestType);
        when(routeDataRequestType.getRequestFromSystem()).thenReturn(mockRequestFromSystem());

        oiRequestMessageReceivedListener.oiRequestMessageReceived(oiRequestMessageReceivedEnvelope);
        verify(cppMessageRepository).save(any(CPPMessage.class));
    }

    private SystemDetailsStructure mockRequestFromSystem() {
        final SystemDetailsStructure systemDetailsStructure = new SystemDetailsStructure();
        final SystemDetailsStructure.DataController dataController = new SystemDetailsStructure.DataController();
        dataController.setValue(FIELD_DATA_CONTROLLER);

        final SystemDetailsStructure.OrganizationalUnitID organizationalUnitID = new SystemDetailsStructure.OrganizationalUnitID();
        organizationalUnitID.setValue(FIELD_ORGANISATION_UNIT_ID);

        systemDetailsStructure.setDataController(dataController);
        systemDetailsStructure.setOrganizationalUnitID(organizationalUnitID);
        return systemDetailsStructure;
    }

    private List<RouteDataStreamType> getMockRouteDataStreamType() {
        final List<RouteDataStreamType> routeDataStreamTypes = new ArrayList<>();

        final RouteDataStreamType routeDataStreamType = new RouteDataStreamType();

        final RouteDataStreamType.RouteSourceSystem routeSourceSystem = new RouteDataStreamType.RouteSourceSystem();
        routeSourceSystem.setValue(FIELD_SOURCE_SYSTEM);
        routeDataStreamType.setRouteSourceSystem(routeSourceSystem);

        final RouteDataStreamType.RouteDestinationSystem routeDestinationSystem = new RouteDataStreamType.RouteDestinationSystem();
        routeDestinationSystem.setValue(FIELD_DESTINATION_SYSTEM);
        routeDataStreamType.setRouteDestinationSystem(routeDestinationSystem);

        routeDataStreamTypes.add(routeDataStreamType);
        return routeDataStreamTypes;
    }
}