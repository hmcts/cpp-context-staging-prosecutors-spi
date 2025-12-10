package uk.gov.moj.cpp.casefilter.azure.service;

import com.microsoft.azure.eventgrid.EventGridClient;
import com.microsoft.azure.eventgrid.TopicCredentials;
import com.microsoft.azure.eventgrid.implementation.EventGridClientImpl;
import com.microsoft.azure.eventgrid.models.EventGridEvent;
import static java.lang.String.format;
import org.joda.time.DateTime;
import uk.gov.moj.cpp.casefilter.azure.pojo.EventType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings({"squid:S2629","squid:S1166"})

public class EventGridService {

    public void publishEventsToCourtStore(String data, Logger logger){
        try {
            // Create an event grid client.
            final String eventGridTopic = System.getenv("EVENT_GRID_TOPIC_KEY");
            final String eventGridEndpoint = System.getenv("EVENT_GRID_TOPIC_ENDPOINT");
            final String eventGriDatadVersion = "1.0";

            final TopicCredentials topicCredentials = new TopicCredentials( eventGridTopic );
            final EventGridClient client = new EventGridClientImpl(topicCredentials);
            logger.info(format("DATA: %s", data));
            final List<EventGridEvent> eventsList = new ArrayList<>();
            logger.info(format("EventType: %s" , EventType.CASE_EJECTED.getType()));
            eventsList.add(new EventGridEvent(UUID.randomUUID().toString(),"CaseEjected",
                    data, EventType.CASE_EJECTED.getType(), DateTime.now(),
                    eventGriDatadVersion));

            final String topicHostName = format("https://%s/", new URI(eventGridEndpoint).getHost());
            client.publishEvents(topicHostName, eventsList);
            logger.info(format("Successfully published to the event grid topic %s" , topicHostName));
        } catch (URISyntaxException exception) {
            logger.info(format("Unexpected Exception caught: %s" , exception.getMessage()));
        }
    }

    public void publishCaseOnCPPEvents(String caseReference, Logger logger) {
        logger.info("Published to the event grid topic " + caseReference);
        try {
            // Create an event grid client.
            final String eventGridTopic = System.getenv("EVENT_GRID_TOPIC_KEY");
            final String eventGridEndpoint = System.getenv("EVENT_GRID_TOPIC_ENDPOINT");

            final TopicCredentials topicCredentials = new TopicCredentials( eventGridTopic );
            final EventGridClient client = new EventGridClientImpl(topicCredentials);

            final List<EventGridEvent> eventsList = new ArrayList<>();
            eventsList.add(new EventGridEvent(UUID.randomUUID().toString(),"CaseOnCpp",
                    caseReference, EventType.CASE_ON_CPP.getType(), DateTime.now(),
                    "1.0"));

            final String topicHostName = String.format("https://%s/", new URI(eventGridEndpoint).getHost());
            client.publishEvents(topicHostName, eventsList);
            logger.info(format("Successfully published to the event grid topic %s" , topicHostName));
        } catch (URISyntaxException exception) {
            logger.info(format("Unexpected Exception caught: %s" , exception.getMessage()));
        }
    }
}
