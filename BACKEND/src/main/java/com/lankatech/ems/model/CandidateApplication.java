package com.lankatech.ems.model;

import com.lankatech.ems.enums.ApplicationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CandidateApplication {

    private int applicationId;
    private int vacancyId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateNic;
    private String resumeNotes;
    private ApplicationStatus status;
    private LocalDate appliedDate;
    private LocalDateTime updatedAt;

    public CandidateApplication() {
    }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getVacancyId() { return vacancyId; }
    public void setVacancyId(int vacancyId) { this.vacancyId = vacancyId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }

    public String getCandidatePhone() { return candidatePhone; }
    public void setCandidatePhone(String candidatePhone) { this.candidatePhone = candidatePhone; }

    public String getCandidateNic() { return candidateNic; }
    public void setCandidateNic(String candidateNic) { this.candidateNic = candidateNic; }

    public String getResumeNotes() { return resumeNotes; }
    public void setResumeNotes(String resumeNotes) { this.resumeNotes = resumeNotes; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
