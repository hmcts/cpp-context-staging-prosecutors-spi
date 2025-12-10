package uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service;

import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.Metadata;

import java.util.Map;
import java.util.UUID;

import org.activiti.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



@ExtendWith(MockitoExtension.class)
public class ActivitiServiceTest {

    @InjectMocks
    private ActivitiService activitiService;


    @Mock
    private RuntimeService runtimeService;

    @Mock
    private Metadata metadata;

    @Captor
    private ArgumentCaptor<String> argumentCaptorString;
    @Captor
    private ArgumentCaptor<Map> argumentCaptorMap;


    private static final UUID cppMessageId = randomUUID();

    @Test
    public void startTimerProcessTest(){
        String userId = randomUUID().toString();
        when(metadata.userId()).thenReturn(of(userId));
        activitiService.startTimerProcess(cppMessageId,metadata);
        verify(runtimeService).startProcessInstanceByKey(argumentCaptorString.capture() , argumentCaptorMap.capture());
        Map<String, String> map = argumentCaptorMap.getValue();
        assertTrue(map.containsKey("cppMessageId"));
        assertTrue(map.containsKey("userId"));
        assertTrue(map.containsValue(cppMessageId));
        assertTrue(map.containsValue(userId));
    }


}