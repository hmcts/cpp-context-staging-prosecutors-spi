package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import static uk.gov.moj.cpp.staging.prosecutors.spi.utils.ZonedDateTimeUtil.removeRightMostZeroes;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.core.random.UUIDGenerator;

import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class C2ITransactionMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(C2ITransactionMetadata.class);

    private String requestId;
    private String transactionId;
    private ZonedDateTime transactionTimestamp;
    private String messageType;

    public C2ITransactionMetadata(String requestId, String transactionId, ZonedDateTime transactionTimestamp, String messageType) {
        this.requestId = requestId;
        this.transactionId = transactionId;
        this.transactionTimestamp = transactionTimestamp;
        this.messageType = messageType;
        LOGGER.info("C2I Transaction Metadata: \n\trequestId {}" +
                "\n\ttransactionId {}" +
                "\n\ttransactionTimestamp {}" +
                "\n\tmessageType {}", requestId, transactionId, ZonedDateTimes.toString(transactionTimestamp), messageType);
    }

    public static C2ITransactionMetadata generateC2ITransactionMetadata(String messageType) {
        return new C2ITransactionMetadata(
                RandomGenerator.integer(1000000, 9999999).next().toString(),
                new UUIDGenerator().next().toString().toUpperCase(),
                removeRightMostZeroes(new UtcClock().now()),
                messageType
        );
    }

    public static C2ITransactionMetadata generateC2ITransactionMetadata(String messageType, ZonedDateTime transactionTimestamp) {
        return new C2ITransactionMetadata(
                RandomGenerator.integer(1000000, 9999999).next().toString(),
                new UUIDGenerator().next().toString().toUpperCase(),
                transactionTimestamp,
                messageType
        );
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public ZonedDateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public String getTransactionTimestampString() {
        return ZonedDateTimes.toString(transactionTimestamp);
    }

    public String getMessageType() {
        return messageType;
    }
}
