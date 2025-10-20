package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.JobPostActivity;
import com.luv2code.jobportal.entity.JobSeekerProfile;
import com.luv2code.jobportal.entity.JobSeekerSave;
import com.luv2code.jobportal.repository.JobSeekerSaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobSeekerSaveService {

    private final JobSeekerSaveRepository jobSeekerSaveRepository;

    public JobSeekerSaveService(JobSeekerSaveRepository jobSeekerSaveRepository) {
        this.jobSeekerSaveRepository = jobSeekerSaveRepository;
    }

    /* ============== READ ============== */

    @Transactional(readOnly = true)
    public List<JobSeekerSave> getCandidatesJob(JobSeekerProfile userProfile) {
        if (userProfile == null) return Collections.emptyList();
        return jobSeekerSaveRepository.findByUserId(userProfile);
    }

    @Transactional(readOnly = true)
    public List<JobSeekerSave> getJobCandidates(JobPostActivity job) {
        if (job == null) return Collections.emptyList();
        return jobSeekerSaveRepository.findByJob(job);
    }

    /** Trả về tập jobPostId mà user đã lưu (tối ưu để set isSaved trên danh sách). */
    @Transactional(readOnly = true)
    public Set<Integer> getSavedJobIdsForUser(JobSeekerProfile userProfile) {
        if (userProfile == null) return Collections.emptySet();
        return jobSeekerSaveRepository.findByUserId(userProfile).stream()
                .map(s -> s.getJob().getJobPostId())
                .collect(Collectors.toSet());
    }

    /** (Tuỳ chọn) Đếm số user đã lưu job này. */
    @Transactional(readOnly = true)
    public long countSavedForJob(JobPostActivity job) {
        if (job == null) return 0;
        return jobSeekerSaveRepository.countByJob(job);
    }

    /* ============== WRITE ============== */

    /** Lưu (save) nếu CHƯA tồn tại — idempotent. Trả về bản ghi hiện có nếu trùng. */
    @Transactional
    public JobSeekerSave addNew(JobSeekerSave save) {
        Assert.notNull(save, "save must not be null");
        Assert.notNull(save.getUserId(), "save.userId must not be null");
        Assert.notNull(save.getJob(), "save.job must not be null");

        Optional<JobSeekerSave> existing =
                jobSeekerSaveRepository.findByUserIdAndJob(save.getUserId(), save.getJob());
        if (existing.isPresent()) {
            return existing.get(); // đã lưu trước đó → không tạo trùng
        }
        return jobSeekerSaveRepository.save(save);
    }

    /** Bỏ lưu (unsave) một job cho user. */
    @Transactional
    public void unsave(JobSeekerProfile user, JobPostActivity job) {
        Assert.notNull(user, "user must not be null");
        Assert.notNull(job, "job must not be null");
        jobSeekerSaveRepository.findByUserIdAndJob(user, job)
                .ifPresent(jobSeekerSaveRepository::delete);
    }

    /** Xóa tất cả bản ghi save theo jobId (dùng khi xóa Job). */
    @Transactional
    public void deleteAllByJobId(int jobPostId) {
        jobSeekerSaveRepository.deleteByJob_JobPostId(jobPostId);
    }

    /** Kiểm tra xem user đã lưu job chưa (tiện cho controller). */
    @Transactional(readOnly = true)
    public boolean existsByUserAndJob(JobSeekerProfile user, JobPostActivity job) {
        if (user == null || job == null) return false;
        return jobSeekerSaveRepository.existsByUserIdAndJob(user, job);
    }
}
