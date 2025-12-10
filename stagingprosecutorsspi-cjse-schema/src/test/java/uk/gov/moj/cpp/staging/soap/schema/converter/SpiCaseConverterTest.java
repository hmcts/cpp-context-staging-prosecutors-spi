package uk.gov.moj.cpp.staging.soap.schema.converter;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceNewCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCase;
import uk.gov.moj.cpp.staging.soap.schema.ObjectUnMarshaller;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@ExtendWith(MockitoExtension.class)
public class SpiCaseConverterTest {


    @InjectMocks
    private final SpiCaseConverter spiCaseConverter = new SpiCaseConverter();

    @Test
    public void DefendantConverterTestshouldContainAnItemIdAndAUrn() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String payload = readFileToString(new File(this.getClass().getClassLoader().getResource("spiInCaseExamples/PoliceCase.xml").getFile()));

        final StdProsPoliceNewCaseStructure stdProsPoliceNewCaseStructure = new ObjectUnMarshaller().getStdProsPoliceNewCaseStructure(payload);

        final PoliceCase policeCase = spiCaseConverter.convert(stdProsPoliceNewCaseStructure.getCase());
        assertThat(policeCase, notNullValue());
        final String policeCaseJsonString = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(policeCase);
        final String expectedJsonString = readFileToString(new File(this.getClass().getClassLoader().getResource("spiInCaseExamples/SingleDefendantSingeOffence.json").getFile()));
        assertEquals(expectedJsonString, policeCaseJsonString, new CustomComparator(JSONCompareMode.STRICT,
                new Customization("defendants[0].id", (o1, o2) -> true)));
    }

}