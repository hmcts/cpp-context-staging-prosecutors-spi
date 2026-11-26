package uk.gov.moj.cpp.staging.prosecutors.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "spi_out_message")
public class SpiOutMessage {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "timestamp")
    private ZonedDateTime timestamp;

    @Column(name = "defendant_reference")
    private String defendantReference;

    @Column(name = "payload")
    private String payload;

    public SpiOutMessage() {
    }

    public SpiOutMessage(final UUID id, final String caseUrn, final ZonedDateTime timestamp, final String defendantReference, final String payload) {
        this.id = id;
        this.caseUrn = caseUrn;
        this.timestamp = timestamp;
        this.defendantReference = defendantReference;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDefendantReference() {
        return defendantReference;
    }

    public void setDefendantReference(final String defendantReference) {
        this.defendantReference = defendantReference;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(final String payload) {
        this.payload = payload;
    }
}
