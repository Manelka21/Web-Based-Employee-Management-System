package com.lankatech.ems.dto.request;

import com.lankatech.ems.util.ValidationRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateApplicationRequest {

    @NotNull
    private Integer vacancyId;

    @NotBlank @Size(min = 2, max = 120)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    private String candidateName;

    @NotBlank @Size(max = 120)
    @Pattern(regexp = ValidationRules.EMAIL, message = ValidationRules.EMAIL_MESSAGE)
    private String candidateEmail;

    @Size(max = 20)
    @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE)
    private String candidatePhone;

    // Needed before the candidate can be HIRED (employees.nic is NOT NULL)
    @Pattern(regexp = ValidationRules.NIC, message = ValidationRules.NIC_MESSAGE)
    private String candidateNic;

    @Size(max = 1000)
    private String resumeNotes;

    // Optional: defaults to today. Can't be in the future.
    private LocalDate appliedDate;

    public Integer getVacancyId() { return vacancyId; }
    public void setVacancyId(Integer vacancyId) { this.vacancyId = vacancyId; }

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

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }
}
