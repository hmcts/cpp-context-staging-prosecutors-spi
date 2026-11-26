package uk.gov.moj.cpp.staging.prosecutors.spi.event.listener;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiPoliceSystemUpdated;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.OiRequestMessageReceived;

import javax.inject.Inject;
import java.util.UUID;

@ServiceComponent(EVENT_LISTENER)
public class OiRequestMessageReceivedListener {

    @Inject
    private CPPMessageRepository cppMessageRepository;

    @Handles("stagingprosecutorsspi.event.spi-oi-request-received")
    public void oiRequestMessageReceived(final Envelope<OiRequestMessageReceived> oiRequestMessageReceivedEnvelope) {
        final CPPMessage cppMessage = getCppMessage(oiRequestMessageReceivedEnvelope.payload());
        cppMessageRepository.save(cppMessage);
    }
    @Handles("stagingprosecutorsspi.event.spi-oi-police-system-updated")
    public void oiPoliceSystemUpdated(final Envelope<OiPoliceSystemUpdated> oiPoliceSystemUpdatedEnvelope) {
        final String policeSystemId = oiPoliceSystemUpdatedEnvelope.payload().getPoliceSystemId();
        final UUID oiId = oiPoliceSystemUpdatedEnvelope.payload().getOiId();
        final CPPMessage cppMessage =  cppMessageRepository.findBy(oiId);
        cppMessage.setPoliceSystemId(policeSystemId);
    }

    private CPPMessage getCppMessage(final OiRequestMessageReceived oiRequestMessageReceived) {

        final CPPMessage cppMessage = new CPPMessage();
        cppMessage.setOiId(oiRequestMessageReceived.getOiId());
        cppMessage.setPoliceSystemId(oiRequestMessageReceived.getPoliceSystemId());
        cppMessage.setCorrelationID(oiRequestMessageReceived.getCorrelationId());

        final SystemDetailsStructure systemDetailsStructure = oiRequestMessageReceived.getRouteDataRequestType() != null ? oiRequestMessageReceived.getRouteDataRequestType().getRequestFromSystem() : null;
        if (null != systemDetailsStructure) {
            final SystemDetailsStructure.DataController dataController = systemDetailsStructure.getDataController();
            if (dataController != null && !isEmpty(dataController.getValue())) {
                cppMessage.setDataController(dataController.getValue());
            }

            final SystemDetailsStructure.OrganizationalUnitID organizationalUnitID = systemDetailsStructure.getOrganizationalUnitID();
            if (organizationalUnitID != null && !isEmpty(organizationalUnitID.getValue())) {
                cppMessage.setOrganizationUnitID(organizationalUnitID.getValue());
            }
        }
        return cppMessage;
    }


}
