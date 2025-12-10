package uk.gov.moj.cpp.casefilter.azure.pojo;

public class Filter {

    private final String courtCentreOUCode;
    private final String prosecutorOUCode;
    private final String caseInitiationCode;
    private final String isLive;

    public Filter(final String courtCentreOUCode,
                  final String prosecutorOUCode,
                  final String caseInitiationCode, final String isLive) {
        this.courtCentreOUCode = courtCentreOUCode;
        this.prosecutorOUCode = prosecutorOUCode;
        this.caseInitiationCode = caseInitiationCode;
        this.isLive = isLive;
    }

    public String getCourtCentreOUCode() {
        return courtCentreOUCode;
    }

    public String getProsecutorOUCode() {
        return prosecutorOUCode;
    }

    public String getCaseInitiationCode() {
        return caseInitiationCode;
    }

    public String getIsLive() {
        return isLive;
    }

    public boolean match(final String reqCourtCentreOUCode,
                         final String reqProsecutorOUCode,
                         final String reqCaseInitiationCode) {
        return reqCourtCentreOUCode.equals(this.courtCentreOUCode) &&
                reqProsecutorOUCode.equals(this.prosecutorOUCode) &&
                reqCaseInitiationCode.equals(this.caseInitiationCode);
    }
}
