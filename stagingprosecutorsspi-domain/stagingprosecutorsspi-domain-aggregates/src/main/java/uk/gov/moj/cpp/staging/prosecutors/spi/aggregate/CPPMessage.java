package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Stream.builder;
import static uk.gov.cjse.schemas.endpoint.types.ExecMode.ASYNCH;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessagePreparedForSending;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.CPPMessageResendFailed;
import uk.gov.moj.cpp.staging.prosecutors.spi.events.UnsuccessfulSyncResponseRecorded;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CppMessageSentSucceessfullyRecorded;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.RetryDelayRequired;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SyncResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class CPPMessage implements Aggregate {

    @SuppressWarnings("squid:S1948")
    private SubmitRequest submitRequest;
    @SuppressWarnings("squid:S1948")
    private boolean sbmissionSuccessfull;
    private int sendTries;
    private static final long serialVersionUID = 100L;
    private final List<Integer> retryResponseList = unmodifiableList(asList(200, 201, 202));


    public Stream<Object> prepareCPPMessageForSending(final String cppMessageId, final String message, final String sourceId, final String destination) {
        final Stream.Builder<Object> streamBuilder = builder();
        if(this.submitRequest == null) {
            final SubmitRequest submitRequestToSend = new SubmitRequest();
            submitRequestToSend.setMessage(message);
            submitRequestToSend.setExecMode(ASYNCH);
            submitRequestToSend.setRequestID(cppMessageId);
            submitRequestToSend.setSourceID(sourceId);
            submitRequestToSend.getDestinationID().add(destination);
            streamBuilder.add(new CPPMessagePreparedForSending(submitRequestToSend));
        }
        return apply(streamBuilder.build());
    }

    @SuppressWarnings("squid:S1602")
    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(CPPMessagePreparedForSending.class).apply(x -> {if(submitRequest != null) { sendTries++;} this.submitRequest = x.getSubmitRequest(); }),
                when(CppMessageSentSucceessfullyRecorded.class).apply(x -> this.sbmissionSuccessfull = true),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> resendMessage(final UUID cppMessageId){
        final Stream.Builder<Object> streamBuilder = builder();
        if (this.submitRequest != null) {
            streamBuilder.add(new CPPMessagePreparedForSending(submitRequest));
        } else {
            streamBuilder.add(new CPPMessageResendFailed(cppMessageId));
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> sendMessageForRetry(final int retryLimit){
        final Stream.Builder<Object> streamBuilder = builder();
        if(this.submitRequest != null && sendTries < retryLimit) {
            streamBuilder.add(new CPPMessagePreparedForSending(submitRequest));
        }
        return apply(streamBuilder.build());
    }

    public Stream<Object> receiveSyncResponse(final SyncResponse syncResponse, final int retryLimit) {
       final Stream.Builder<Object> streamBuilder = builder();
       if(!sbmissionSuccessfull) {
           if (syncResponse.getResponseCode() == 1) {
               streamBuilder.add(new CppMessageSentSucceessfullyRecorded(syncResponse));
           } else {
               streamBuilder.add(new UnsuccessfulSyncResponseRecorded(syncResponse));
               if (retryResponseList.contains(syncResponse.getResponseCode()) && sendTries < retryLimit) {
                   streamBuilder.add(new RetryDelayRequired(syncResponse.getRequestID()));
               }
           }
       }
       return apply(streamBuilder.build());
    }
}
