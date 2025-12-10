package uk.gov.moj.cpp.casefilter.azure.pojo;

public class CJSEMetaDataResponse {

    private CJSEMetaData cjseMetaData;

    public CJSEMetaDataResponse(final CJSEMetaData cjseMetaData) {
        this.cjseMetaData = cjseMetaData;
    }

    public CJSEMetaData getCjseMetaData() {
        return cjseMetaData;
    }
}
