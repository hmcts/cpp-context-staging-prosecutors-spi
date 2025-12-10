package uk.gov.moj.cpp.staging.prosecutors.domain.schema;

import java.io.IOException;
import java.io.InputStream;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JsonSchemaChecker {

    private String validDocumentPath;
    private Schema schema;

    private JsonSchemaChecker(final ReferencedSchema referencedSchema, final String validDocumentPath) {

        this.schema = referencedSchema.getSchema();
        this.validDocumentPath = validDocumentPath;

        assertSchemaValidatesSuccessfullyWithValidDocument();
    }

    public static JsonSchemaChecker forSchema(final ReferencedSchema referencedSchema, final String validDocumentPath) {

        return new JsonSchemaChecker(referencedSchema, validDocumentPath);
    }

    protected Schema getSchema() {
        return schema;
    }

    JSONObject getValidDocument() {

        try (InputStream documentInputStream = getClass().getClassLoader().getResourceAsStream(validDocumentPath)) {

            return new JSONObject(new JSONTokener(documentInputStream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonPropertyChecker checkProperty(final String property) {

        return JsonPropertyChecker.of(this, property);
    }

    private void assertSchemaValidatesSuccessfullyWithValidDocument() {

        schema.validate(getValidDocument());
    }
}
