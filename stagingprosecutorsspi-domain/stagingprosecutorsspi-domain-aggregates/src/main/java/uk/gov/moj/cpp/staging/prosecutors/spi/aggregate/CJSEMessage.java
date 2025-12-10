package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseDuplicateRequestMessageReceived;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessage;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseMessageReceivedWithMdiErrors;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CjseRequestMessageReceived;

import java.util.stream.Stream;

public class CJSEMessage implements Aggregate {

    private String requestId;
    private String message;
    private static final long serialVersionUID = 100L;

    public Stream<Object> receiveCjseMessage(final CjseMessage cjseMessage) {
        if (cjseMessage.getMdiError() != null) {
            return apply(of(CjseMessageReceivedWithMdiErrors.cjseMessageReceivedWithMdiErrors()
                    .withRequestId(cjseMessage.getRequestId())
                    .withCjseMessage(cjseMessage)
                    .build()
            ));
        } else if (requestId != null) {
            return apply(of(CjseDuplicateRequestMessageReceived.cjseDuplicateRequestMessageReceived()
                    .withRequestId(cjseMessage.getRequestId())
                    .withCjseMessage(cjseMessage)
                    .build()
            ));
        } else {
            return apply(of(CjseRequestMessageReceived.cjseRequestMessageReceived()
                    .withRequestId(cjseMessage.getRequestId())
                    .withCjseMessage(cjseMessage)
                    .build()
            ));
        }
    }

    @SuppressWarnings("squid:S1602")
    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(CjseRequestMessageReceived.class).apply(cjseRequestMessageReceived -> {
                    this.requestId = cjseRequestMessageReceived.getRequestId();
                    this.message = cjseRequestMessageReceived.getCjseMessage().getMessage();
                }),
                when(CjseDuplicateRequestMessageReceived.class).apply(cjseDuplicateRequestMessageReceived -> {
                }),
                when(CjseMessageReceivedWithMdiErrors.class).apply(cjseMessageReceivedWithMdiErrors -> {
                })
        );
    }


    public String getRequestId() {
        return requestId;
    }

    public String getMessage() {
        return message;
    }

}
