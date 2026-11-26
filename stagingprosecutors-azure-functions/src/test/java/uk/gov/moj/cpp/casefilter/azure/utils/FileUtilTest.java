package uk.gov.moj.cpp.casefilter.azure.utils;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getJsonObject;
import static uk.gov.moj.cpp.casefilter.azure.utils.FileUtil.getPathValue;

import java.util.Optional;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class FileUtilTest {

    @Test
    public void shouldGetPathValueSuccessfully() {
        final JsonObject jsonObject = getJsonObject("{\"film\": {\"actor\": {\"name\": \"Will\"}}}");

        Optional<String> actor = getPathValue(jsonObject, "film.actor");
        assertThat(actor.isPresent(), is(TRUE));
        Optional<String> pathValue = getPathValue(jsonObject, "film.actor.name");

        assertThat(pathValue.isPresent(), is(TRUE));
        assertThat(pathValue.get(), is("Will"));
    }

    @Test
    public void shouldGetPathValueFromArraySuccessfully() {
        final JsonObject jsonObject = getJsonObject("{\"film\": {\"actors\": [{\"name\": \"Will\"}]}}");

        Optional<String> pathValue = getPathValue(jsonObject, "film.actors[0].name");

        assertThat(pathValue.isPresent(), is(TRUE));
        assertThat(pathValue.get(), is("Will"));
    }

    @Test
    public void shouldReturnEmptyForNonExistingPath() {
        final JsonObject jsonObject = getJsonObject("{\"film\": {\"actor\": {\"name\": \"Will\"}}}");

        Optional<String> pathValue = getPathValue(jsonObject, "film.name");

        assertThat(pathValue.isPresent(), is(FALSE));
    }
}