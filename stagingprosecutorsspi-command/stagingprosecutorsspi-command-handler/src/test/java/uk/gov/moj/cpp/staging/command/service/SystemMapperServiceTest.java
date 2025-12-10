package uk.gov.moj.cpp.staging.command.service;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemMapperServiceTest {

    private static final String MOCK_URN = "Mock URN";
    @InjectMocks
    private SystemMapperService systemMapperService;
    @Mock
    private SystemUserProvider systemUserProvider;
    @Mock
    private SystemIdMapperClient systemIdMapperClient;
    @Mock
    private SystemIdMapping systemIdMapping;
    @Mock
    private AdditionResponse additionResponse;
    @Captor
    private ArgumentCaptor<SystemIdMap> argumentCaptor;


    @Test
    public void shouldReturnCorrectCaseIdWhenCaseIdExists() {
        final UUID idExpected = randomUUID();
        final Optional<SystemIdMapping> systemIdMappingOptional = Optional.of(systemIdMapping);

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));
        when(systemIdMapperClient.findBy(eq(MOCK_URN), any(), any(), any())).thenReturn(systemIdMappingOptional);
        when(systemIdMapping.getTargetId()).thenReturn(idExpected);

        final UUID idActual = systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(MOCK_URN);

        assertThat(idActual, is(idExpected));
    }

    @Test
    public void shouldAddAndReturnCaseIdWithCorrectURNToSystemMapperWhenCaseIdDoesNotExist() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));
        when(systemIdMapperClient.findBy(eq(MOCK_URN), any(), any(), any())).thenReturn(Optional.empty());
        when(systemIdMapperClient.add(any(), any())).thenReturn(additionResponse);
        when(additionResponse.isSuccess()).thenReturn(true);

        final UUID idActual = systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(MOCK_URN);

        verify(systemIdMapperClient).add(argumentCaptor.capture(), any());
        final SystemIdMap systemIdMap = argumentCaptor.getValue();

        final String urnActual = systemIdMap.getSourceId();
        final UUID idExpected = systemIdMap.getTargetId();

        assertThat(urnActual, is(MOCK_URN));
        assertThat(idActual, is(idExpected));
    }

    @Test
    public void shouldThrowErrorWhenAddingNewCaseIdFails() {
        final Optional<SystemIdMapping> systemIdMappingOptional = Optional.empty();

        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));
        when(systemIdMapperClient.findBy(eq(MOCK_URN), any(), any(), any())).thenReturn(systemIdMappingOptional);
        when(systemIdMapperClient.add(any(), any())).thenReturn(additionResponse);
        when(additionResponse.isSuccess()).thenReturn(false);

        var e = assertThrows(Exception.class, () ->  systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(MOCK_URN));
        assertThat(e.getMessage(), is(format("Unable to creating mapping for input String %s to a uuid", MOCK_URN)));
    }

    @Test
    public void shouldThrowErrorWhenContextSystemIdNotPresent() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.empty());

        var e = assertThrows(Exception.class, () ->  systemMapperService.getCaseIdForPtiURNAndOriginatingOrganisation(MOCK_URN));
        assertThat(e.getMessage(), is(SystemMapperService.CONTEXT_SYSTEM_USER_ID_IS_NOT_PRESENT));
    }

}
