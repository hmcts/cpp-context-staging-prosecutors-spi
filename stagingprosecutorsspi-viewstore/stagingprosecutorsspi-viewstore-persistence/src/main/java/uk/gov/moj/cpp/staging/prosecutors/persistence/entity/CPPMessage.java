package uk.gov.moj.cpp.staging.prosecutors.persistence.entity;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cpp_message")
public class CPPMessage {

    @Id
    @Column(name = "oi_id")
    private UUID oiId;

    @Column(name = "pti_urn")
    private String ptiUrn;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "police_system_id")
    private String policeSystemId;

    @Column(name = "correlation_id")
    private String correlationID;

    @Column(name = "organization_unit_id")
    private String organizationUnitID;

    @Column(name = "data_controller")
    private String dataController;

    public CPPMessage() {
    }

    public CPPMessage(final UUID oiId, final String ptiUrn, final UUID caseId, final String policeSystemId, String correlationID) {
        this.oiId = oiId;
        this.ptiUrn = ptiUrn;
        this.caseId = caseId;
        this.policeSystemId = policeSystemId;
        this.correlationID = correlationID;
    }

    public UUID getOiId() {
        return oiId;
    }

    public void setOiId(final UUID oiId) {
        this.oiId = oiId;
    }

    public String getPtiUrn() {
        return ptiUrn;
    }

    public void setPtiUrn(final String ptiUrn) {
        this.ptiUrn = ptiUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public String getPoliceSystemId() {
        return policeSystemId;
    }

    public void setPoliceSystemId(final String policeSystemId) {
        this.policeSystemId = policeSystemId;
    }

    public String getCorrelationID() {
        return correlationID;
    }

    public void setCorrelationID(final String correlationID) {
        this.correlationID = correlationID;
    }

    public String getOrganizationUnitID() {
        return organizationUnitID;
    }

    public void setOrganizationUnitID(final String organizationUnitID) {
        this.organizationUnitID = organizationUnitID;
    }

    public String getDataController() {
        return dataController;
    }

    public void setDataController(final String dataController) {
        this.dataController = dataController;
    }

}
