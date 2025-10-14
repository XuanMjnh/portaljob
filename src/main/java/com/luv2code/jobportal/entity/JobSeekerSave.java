package com.luv2code.jobportal.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(
        name = "job_seeker_save",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "job"})
        }
)
public class JobSeekerSave implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "user_account_id", nullable = false)
    private JobSeekerProfile userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job", referencedColumnName = "jobPostId", nullable = false)
    private JobPostActivity job;

    public JobSeekerSave() {}

    public JobSeekerSave(JobSeekerProfile userId, JobPostActivity job) {
        this.userId = userId;
        this.job = job;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public JobSeekerProfile getUserId() {
        return userId;
    }

    public void setUserId(JobSeekerProfile userId) {
        this.userId = userId;
    }

    public JobPostActivity getJob() {
        return job;
    }

    public void setJob(JobPostActivity job) {
        this.job = job;
    }

    @Override
    public String toString() {
        return "JobSeekerSave{" +
                "id=" + id +
                ", userId=" + (userId != null ? userId.getUserAccountId() : "null") +
                ", job=" + (job != null ? job.getJobPostId() : "null") +
                '}';
    }
}
