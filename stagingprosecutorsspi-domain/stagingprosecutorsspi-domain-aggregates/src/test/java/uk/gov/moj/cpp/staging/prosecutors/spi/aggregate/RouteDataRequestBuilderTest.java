package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiResult;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RouteDataRequestBuilderTest {

    private static final String DATACONTROLLER = "Data Controller";
    private static final String DESTINATIONSYSTEMID = "Destination SystemId";
    private static final String PAYLOAD = "payload";
    private static final String PTIURN = "ptiUrn";
    private static final String CPPSYSTEMID = "cppSystemId";

    @InjectMocks
    private RouteDataRequestBuilder routeDataRequestBuilder;

    @Test
    public void shouldBuilderRouteDataRequest() {

        final SpiResult spiResult = new SpiResult(DATACONTROLLER, DESTINATIONSYSTEMID, null, PAYLOAD, PTIURN, null);
        final UUID correlationId = randomUUID();
        final RouteDataRequestType routeDataRequestType = routeDataRequestBuilder.prepareRouteDataRequest(spiResult, CPPSYSTEMID, correlationId);

        assertNotNull(routeDataRequestType);
        assertThat(routeDataRequestType.getDataStream().getDataStreamContent(), is(PAYLOAD));
        assertThat(routeDataRequestType.getDataStream().getSystemDataStreamID().getValue(), is(correlationId.toString()));
        assertThat(routeDataRequestType.getRoutes().getRoute().get(0).getRouteDestinationSystem().getValue(), is(DESTINATIONSYSTEMID));
        assertThat(routeDataRequestType.getRoutes().getRoute().get(0).getRouteSourceSystem().getValue(), is(CPPSYSTEMID));
        assertThat(routeDataRequestType.getRequestFromSystem().getDataController().getValue(), is(DATACONTROLLER));
        assertThat(routeDataRequestType.getRequestFromSystem().getCorrelationID(), is(correlationId.toString()));

    }
}