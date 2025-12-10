package uk.gov.moj.cpp.casefilter.azure.utils;

import static com.jayway.jsonpath.JsonPath.read;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.StringReader;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    private FileUtil(){}

    public static JsonObject getJsonObject(final String jsonAsString) {
        final JsonObject payload;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonAsString))) {
            payload = jsonReader.readObject();
        }
        return payload;
    }

    public static Optional<String> getPathValue(final JsonObject jsonObject, final String path) {
        try {
            return of(read(jsonObject.toString(), path).toString());
        }
        catch (final PathNotFoundException ex) {
            LOGGER.error(format("%s not found", path), ex);
        }
        return empty();
    }
}
