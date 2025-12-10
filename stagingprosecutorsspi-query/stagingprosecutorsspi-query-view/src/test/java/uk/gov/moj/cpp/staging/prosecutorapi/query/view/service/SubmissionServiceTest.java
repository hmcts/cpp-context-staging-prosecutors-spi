package uk.gov.moj.cpp.staging.prosecutorapi.query.view.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.staging.prosecutors.persistence.entity.CPPMessage;
import uk.gov.moj.cpp.staging.prosecutors.persistence.repository.CPPMessageRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceTest {
    final String ptiUrn = "ptiUrn";
    @InjectMocks
    private CPPMessageService service;
    @Mock
    private CPPMessageRepository repository;

    @Test
    public void getOptionalCPPMessageByIdIfReturnedByRepository() {

        final CPPMessage expectedCppMessage = new CPPMessage(randomUUID(), ptiUrn, randomUUID(), "police system id", "correlation id");
        when(repository.findByPtiUrn(ptiUrn)).thenReturn(Arrays.asList(expectedCppMessage));

        List<CPPMessage> cppMessages = service.getCPPMessages(ptiUrn);

        assertThat(cppMessages.size(), is(1));
    }

    @Test
    public void getEmptyIfSubmissionNotReturnedByRepository() {

        when(repository.findByPtiUrn(ptiUrn)).thenReturn(Collections.EMPTY_LIST);

        List<CPPMessage> cppMessages = service.getCPPMessages(ptiUrn);

        assertThat(cppMessages.size(), is(0));

    }

}