package uk.gov.moj.cpp.casefilter.azure.entity;

import static java.lang.String.format;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import com.microsoft.azure.storage.table.TableServiceEntity;

@SuppressWarnings({"squid:S2384"})
public class EjectedOrFilteredCase extends TableServiceEntity {

    private String caseReference;
    private String caseInitiationCode;
    private String prosecutorOUCode;
    private String courtCentreOUCode;
    private boolean isFiltered;
    private boolean isEjected;
    private String caseId;
    private Date hearingDate;
    private String summonsCode;

    public EjectedOrFilteredCase(final String prosecutorOUCode, final String caseReference) {
        this.partitionKey = prosecutorOUCode;
        this.rowKey = format("%s_%s", caseReference, prosecutorOUCode);
    }

    public EjectedOrFilteredCase() {
        //default constructor
    }


    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(final String caseReference) {
        this.caseReference = caseReference;
    }

    public String getCaseInitiationCode() {
        return caseInitiationCode;
    }

    public void setCaseInitiationCode(final String caseInitiationCode) {
        this.caseInitiationCode = caseInitiationCode;
    }

    public String getProsecutorOUCode() {
        return prosecutorOUCode;
    }

    public void setProsecutorOUCode(final String prosecutorOUCode) {
        this.prosecutorOUCode = prosecutorOUCode;
    }

    public String getCourtCentreOUCode() {
        return courtCentreOUCode;
    }

    public void setCourtCentreOUCode(final String courtCentreOUCode) {
        this.courtCentreOUCode = courtCentreOUCode;
    }

    public boolean getIsFiltered() {
        return isFiltered;
    }

    public void setIsFiltered(final boolean filtered) {
        isFiltered = filtered;
    }

    public boolean getIsEjected() {
        return isEjected;
    }

    public void setIsEjected(final boolean ejected) {
        isEjected = ejected;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(final String caseId) {
        this.caseId = caseId;
    }

    public Date getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final Date hearingDate) {
        this.hearingDate = hearingDate;
    }

    public String getSummonsCode() {
        return summonsCode;
    }

    public void setSummonsCode(final String summonsCode) {
        this.summonsCode = summonsCode;
    }


    public void setHearingDateTime(final LocalDate date, final LocalTime time) {
        final LocalDateTime localDateTime = LocalDateTime.of(date, time);
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
        setHearingDate(Date.from(zonedDateTime.toInstant()));
    }

    @Override
    public String toString() {
        return "EjectedOrFilteredCase{" +
                "caseReference='" + caseReference + '\'' +
                ", caseInitiationCode='" + caseInitiationCode + '\'' +
                ", prosecutorOUCode='" + prosecutorOUCode + '\'' +
                ", courtCentreOUCode='" + courtCentreOUCode + '\'' +
                ", isFiltered=" + isFiltered +
                ", isEjected=" + isEjected +
                ", caseId='" + caseId + '\'' +
                ", hearingDateTime=" + hearingDate +
                ", summonsCode='" + summonsCode + '\'' +
                ", partitionKey='" + partitionKey + '\'' +
                ", rowKey='" + rowKey + '\'' +
                ", etag='" + etag + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }

}
