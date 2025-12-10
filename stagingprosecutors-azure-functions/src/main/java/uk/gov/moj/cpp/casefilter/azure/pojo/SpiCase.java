package uk.gov.moj.cpp.casefilter.azure.pojo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public class SpiCase {

    private String courtCentreOUCode;
    private String prosecutorOUCode;
    private String caseInitiationCode;
    private LocalDate hearingDate;
    private LocalTime hearingTime;
    private String urn;
    private Optional<String> summonsCode;

    public String getProsecutorOUCode() {
        return prosecutorOUCode;
    }

    public String getCaseInitiationCode() {
        return caseInitiationCode;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public LocalTime getHearingTime() {
        if(hearingTime == null){
            return LocalTime.MAX;
        }
        return hearingTime;
    }

    public String getUrn() {
        return urn;
    }

    public Optional<String> getSummonsCode() {
        return summonsCode;
    }

    public String getCourtCentreOUCode() {
        return courtCentreOUCode;
    }

    public LocalDateTime getHearingDateTime() {
        if(hearingTime == null) {
            return  LocalDateTime.of(hearingDate, LocalTime.MAX);
        }
        return LocalDateTime.of(hearingDate, hearingTime);
    }

    public static final class SpiCaseBuilder {
        private String courtCentreOUCode;
        private String prosecutorOUCode;
        private String caseInitiationCode;
        private LocalDate hearingDate;
        private LocalTime hearingTime;
        private String urn;
        private Optional<String> summonsCode;

        private SpiCaseBuilder() {
        }

        public static SpiCaseBuilder aSpiCase() {
            return new SpiCaseBuilder();
        }

        public SpiCaseBuilder withCourtCentreOUCode(String courtCentreOUCode) {
            this.courtCentreOUCode = courtCentreOUCode;
            return this;
        }

        public SpiCaseBuilder withProsecutorOUCode(String prosecutorOUCode) {
            this.prosecutorOUCode = prosecutorOUCode;
            return this;
        }

        public SpiCaseBuilder withCaseInitiationCode(String caseInitiationCode) {
            this.caseInitiationCode = caseInitiationCode;
            return this;
        }

        public SpiCaseBuilder withHearingDate(LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public SpiCaseBuilder withHearingTime(LocalTime hearingTime) {
            this.hearingTime = hearingTime;
            return this;
        }

        public SpiCaseBuilder withUrn(String urn) {
            this.urn = urn;
            return this;
        }

        public SpiCaseBuilder withSummonsCode(Optional<String> summonsCode) {
            this.summonsCode = summonsCode;
            return this;
        }

        public SpiCaseBuilder but() {
            return aSpiCase().withCourtCentreOUCode(courtCentreOUCode).withProsecutorOUCode(prosecutorOUCode).withCaseInitiationCode(caseInitiationCode).withHearingDate(hearingDate).withHearingTime(hearingTime).withUrn(urn).withSummonsCode(summonsCode);
        }

        public SpiCase build() {
            final SpiCase spiCase = new SpiCase();
            spiCase.hearingDate = this.hearingDate;
            spiCase.hearingTime = this.hearingTime;
            spiCase.courtCentreOUCode = this.courtCentreOUCode;
            spiCase.summonsCode = this.summonsCode;
            spiCase.caseInitiationCode = this.caseInitiationCode;
            spiCase.prosecutorOUCode = this.prosecutorOUCode;
            spiCase.urn = this.urn;
            return spiCase;
        }
    }
}
