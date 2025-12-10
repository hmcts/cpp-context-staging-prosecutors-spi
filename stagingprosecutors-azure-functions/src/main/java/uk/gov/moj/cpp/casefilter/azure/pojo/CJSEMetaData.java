package uk.gov.moj.cpp.casefilter.azure.pojo;

public class CJSEMetaData {

    private String caseReference;
    private String caseInitiationCode;
    private String prosecutorOUCode;
    private String courtCenterOUCode;
    private String timeOfHearing;
    private String dateOfHearing;
    private String summons;
    private Boolean mdiFailure;
    private Boolean isAsyncResponse;

    public CJSEMetaData(final Boolean mdiFailure, final Boolean isAsyncResponse) {
        this.mdiFailure = mdiFailure;
        this.isAsyncResponse = isAsyncResponse;
    }



    public CJSEMetaData(final String caseReference, final String caseInitiationCode,
                        final String prosecutorOUCode, final String courtCenterOUCode,
                        final String timeOfHearing, final String dateOfHearing,
                        final String summons) {
        this.caseReference = caseReference;
        this.caseInitiationCode = caseInitiationCode;
        this.prosecutorOUCode = prosecutorOUCode;
        this.courtCenterOUCode = courtCenterOUCode;
        this.timeOfHearing = timeOfHearing;
        this.dateOfHearing = dateOfHearing;
        this.summons = summons;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public String getCaseInitiationCode() {
        return caseInitiationCode;
    }

    public String getProsecutorOUCode() {
        return prosecutorOUCode;
    }

    public String getCourtCenterOUCode() {
        return courtCenterOUCode;
    }

    public String getTimeOfHearing() {
        return timeOfHearing;
    }

    public String getDateOfHearing() {
        return dateOfHearing;
    }

    public String getSummons() {
        return summons;
    }

    public Boolean isMdiFailure() {
        return mdiFailure;
    }

    public Boolean getIsAsyncResponse() {
        return isAsyncResponse;
    }
}
