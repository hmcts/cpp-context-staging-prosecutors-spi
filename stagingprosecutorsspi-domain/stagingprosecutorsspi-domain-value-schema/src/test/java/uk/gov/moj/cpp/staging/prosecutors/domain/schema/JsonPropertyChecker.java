package uk.gov.moj.cpp.staging.prosecutors.domain.schema;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.everit.json.schema.ValidationException;
import org.json.JSONObject;

public class JsonPropertyChecker {

    private final JsonSchemaChecker jsonSchemaChecker;
    private final String property;

    private JsonPropertyChecker(JsonSchemaChecker jsonSchemaChecker, String property) {
        this.jsonSchemaChecker = jsonSchemaChecker;
        this.property = property;
    }

    public static JsonPropertyChecker of(JsonSchemaChecker schema, String property) {
        return new JsonPropertyChecker(schema, property);
    }

    public JsonPropertyChecker isMandatory() {

        JSONObject modifiedDocument = jsonSchemaChecker.getValidDocument();

        modifiedDocument.remove(property);

        validateExpectingMessage(modifiedDocument, format("#: required key [%s] not found", property));

        return this;
    }

    public JsonPropertyChecker allowsValues(final String... validValues) {

        JSONObject document = jsonSchemaChecker.getValidDocument();

        Stream.of(validValues).forEach(value -> {
            document.put(property, value);
            jsonSchemaChecker.getSchema().validate(document);
        });

        return this;
    }

    public JsonPropertyChecker rejectsValues(final String... invalidValues) {

        Stream.of(invalidValues).forEach(
                value -> {
                    JSONObject document = jsonSchemaChecker.getValidDocument();
                    document.put(property, value == null ? JSONObject.NULL : value);
                    validateExpectingException(document, "Should not allow: " + value);
                }
        );

        return this;
    }

    public JsonPropertyChecker rejectsEmptyAndNull() {
        return rejectsValues("", null);
    }

    private void validateExpectingException(final JSONObject document, final String message) {

        try {
            jsonSchemaChecker.getSchema().validate(document);
        } catch (ValidationException e) {

            return;
        }

        fail(format("Property '%s': %s", property, message));
    }

    private void validateExpectingMessage(final JSONObject document, final String expectedMessage) {

        String actualExceptionMessage = null;

        try {
            jsonSchemaChecker.getSchema().validate(document);
        } catch (ValidationException e) {

            actualExceptionMessage = e.getMessage();
        }

        assertEquals(format("Checking property '%s'", property), expectedMessage, actualExceptionMessage);
    }
}
