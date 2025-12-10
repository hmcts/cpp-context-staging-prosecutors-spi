package uk.gov.moj.cpp.staging.prosecutors.spi.event.helper;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentreWithLJA;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SessionDay;

import java.util.List;
import java.util.UUID;

public class PublicPoliceResultGenerated {
    private UUID id;
    private List<SessionDay> sessionDays;
    private CourtCentreWithLJA courtCentreWithLJA;
    private UUID caseId;
    private String urn;
    private CaseDefendant defendant;


    public static PublicPoliceResultGenerated publicPoliceResultGenerated(){
        return new PublicPoliceResultGenerated();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public List<SessionDay> getSessionDays() {
        return sessionDays;
    }

    public void setSessionDays(List<SessionDay> sessionDays) {
        this.sessionDays = sessionDays;
    }

    public CourtCentreWithLJA getCourtCentreWithLJA() {
        return courtCentreWithLJA;
    }

    public void setCourtCentreWithLJA(CourtCentreWithLJA courtCentreWithLJA) {
        this.courtCentreWithLJA = courtCentreWithLJA;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public CaseDefendant getDefendant() {
        return defendant;
    }

    public void setDefendant(CaseDefendant defendant) {
        this.defendant = defendant;
    }
}
