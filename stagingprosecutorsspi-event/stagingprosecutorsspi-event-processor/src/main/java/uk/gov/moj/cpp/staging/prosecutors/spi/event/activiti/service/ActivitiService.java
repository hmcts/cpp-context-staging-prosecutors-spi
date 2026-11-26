package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.messaging.Metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.activiti.engine.RuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivitiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiService.class);
    @Inject
    private RuntimeService runtimeService;

    @Inject
    @Value(key = "cjse.retry.duration", defaultValue = "PT5M")
    private String retryDuration;

    @SuppressWarnings("squid:S3655")
    public void startTimerProcess(final UUID cppMessageId, final Metadata metadata) {

        LOGGER.info("Starting timer process for messageId: {}", cppMessageId);
        final Map<String, Object> processMap = new HashMap<>();
        processMap.put("cppMessageId", cppMessageId);
        processMap.put("SPI_RETRY_TIMER_VALUE", retryDuration);
        processMap.put("userId", metadata.userId().get());
        runtimeService.startProcessInstanceByKey("timer_flow", processMap);
    }

}
