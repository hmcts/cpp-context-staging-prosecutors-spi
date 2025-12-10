package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import uk.gov.cjse.schemas.common.businessentities.DataStreamStructure;
import uk.gov.cjse.schemas.common.businessentities.ObjectFactory;
import uk.gov.cjse.schemas.common.operations.MessageType;
import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;

import java.io.Serializable;
import java.util.UUID;

public class RouteDataRequestBuilder implements Serializable {

    public RouteDataRequestType prepareRouteDataRequest(final SpiResult spiResult, final String cppSystemId, final UUID correlationId) {

        final RouteDataRequestType routeDataRequestType = new RouteDataRequestType();
        routeDataRequestType.setRequestResponse(MessageType.REQUEST);
        routeDataRequestType.setDataStream(getDataStream(spiResult.getPayload(), cppSystemId, correlationId));
        routeDataRequestType.setRoutes(getRoutes(cppSystemId, spiResult.getDestinationSystemId()));
        routeDataRequestType.setRequestFromSystem(getRequestFromSystem(spiResult, correlationId, cppSystemId));

        return routeDataRequestType;

    }

    private RouteDataRequestType.Routes getRoutes(final String cppSystemId, final String destinationSystemId) {
        final uk.gov.cjse.schemas.common.operations.ObjectFactory objectFactory = new uk.gov.cjse.schemas.common.operations.ObjectFactory();

        final RouteDataStreamType.RouteDestinationSystem routeDestinationSystem = objectFactory.createRouteDataStreamTypeRouteDestinationSystem();
        routeDestinationSystem.setValue(destinationSystemId);

        final RouteDataStreamType.RouteSourceSystem routeSourceSystem = objectFactory.createRouteDataStreamTypeRouteSourceSystem();
        routeSourceSystem.setValue(cppSystemId);

        final RouteDataRequestType.Routes routes = objectFactory.createRouteDataRequestTypeRoutes();
        final RouteDataStreamType routeDataStreamType = objectFactory.createRouteDataStreamType();
        routeDataStreamType.setRouteID("1");
        routeDataStreamType.setRouteDestinationSystem(routeDestinationSystem);
        routeDataStreamType.setRouteSourceSystem(routeSourceSystem);

        routes.getRoute().add(routeDataStreamType);
        return routes;
    }

    private DataStreamStructure getDataStream(final String resultPayload, final String cppSystemId, final UUID correlationId) {
        final ObjectFactory objectFactory = new ObjectFactory();
        final DataStreamStructure dataStream = objectFactory.createDataStreamStructure();

        final DataStreamStructure.System dataStreamStructureSystem = objectFactory.createDataStreamStructureSystem();
        dataStreamStructureSystem.setValue(cppSystemId);
        dataStream.setSystem(dataStreamStructureSystem);

        final DataStreamStructure.DataStreamType dataStreamStructureDataStreamType = objectFactory.createDataStreamStructureDataStreamType();
        dataStreamStructureDataStreamType.setValue("SPIResults");
        dataStream.setDataStreamType(dataStreamStructureDataStreamType);

        final DataStreamStructure.SystemDataStreamID dataStreamStructureSystemDataStreamID = objectFactory.createDataStreamStructureSystemDataStreamID();
        dataStreamStructureSystemDataStreamID.setValue(correlationId.toString());// same as correlation id
        dataStream.setSystemDataStreamID(dataStreamStructureSystemDataStreamID);

        dataStream.setContentType("text/xml");

        dataStream.setDataStreamContent(resultPayload);

        return dataStream;
    }


    private SystemDetailsStructure getRequestFromSystem(final SpiResult spiResult, final UUID correlationId, final String cppSystemId) {
        final uk.gov.cjse.schemas.common.operations.ObjectFactory objectFactory = new uk.gov.cjse.schemas.common.operations.ObjectFactory();
        final SystemDetailsStructure systemDetailsStructure = objectFactory.createSystemDetailsStructure();

        final SystemDetailsStructure.SystemID systemID = objectFactory.createSystemDetailsStructureSystemID();
        systemID.setValue(cppSystemId);

        systemDetailsStructure.setCorrelationID(correlationId.toString());
        systemDetailsStructure.setSystemID(systemID);
        systemDetailsStructure.setDataController(getDataController(spiResult, objectFactory));
        systemDetailsStructure.setOrganizationalUnitID(getOrganizationalUnitID(spiResult, objectFactory));

        return systemDetailsStructure;
    }

    private SystemDetailsStructure.DataController getDataController(final SpiResult spiResult, final uk.gov.cjse.schemas.common.operations.ObjectFactory objectFactory) {

        if (!isEmpty(spiResult.getDataController())) {
            final SystemDetailsStructure.DataController dataController = objectFactory.createSystemDetailsStructureDataController();
            dataController.setValue(spiResult.getDataController());
            return dataController;
        } else {
            return null;
        }
    }

    private SystemDetailsStructure.OrganizationalUnitID getOrganizationalUnitID(final SpiResult spiResult, final uk.gov.cjse.schemas.common.operations.ObjectFactory objectFactory) {

        if (!isEmpty(spiResult.getOrganizationalUnitID())) {
            final SystemDetailsStructure.OrganizationalUnitID organizationalUnitID = objectFactory.createSystemDetailsStructureOrganizationalUnitID();
            organizationalUnitID.setValue(spiResult.getOrganizationalUnitID());
            return organizationalUnitID;
        } else {
            return null;
        }
    }

}