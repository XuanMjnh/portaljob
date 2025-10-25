package com.luv2code.jobportal.services;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import com.luv2code.jobportal.repository.JobSeekerApplyRepository;
import com.luv2code.jobportal.repository.JobSeekerSaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class JobPostActivityService {

    private final JobPostActivityRepository jobPostActivityRepository;
    private final JobSeekerApplyRepository jobSeekerApplyRepository;
    private final JobSeekerSaveRepository jobSeekerSaveRepository;
    private final UsersService usersService; // để kiểm tra quyền sở hữu & lấy profile hiện tại

    public JobPostActivityService(JobPostActivityRepository jobPostActivityRepository,
                                  JobSeekerApplyRepository jobSeekerApplyRepository,
                                  JobSeekerSaveRepository jobSeekerSaveRepository,
                                  UsersService usersService) {
        this.jobPostActivityRepository = jobPostActivityRepository;
        this.jobSeekerApplyRepository = jobSeekerApplyRepository;
        this.jobSeekerSaveRepository = jobSeekerSaveRepository;
        this.usersService = usersService;
    }

    /* ===================== CRUD / QUERY ===================== */

    @Transactional
    public JobPostActivity addNew(JobPostActivity jobPostActivity) {
        return jobPostActivityRepository.save(jobPostActivity);
    }

    public JobPostActivity getOne(int id) {
        return jobPostActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public List<JobPostActivity> getAll() {
        return jobPostActivityRepository.findAll();
    }

    /**
     * Tìm kiếm job theo từ khóa, địa điểm, loại hình (type), chế độ làm việc (remote) và mốc thời gian.
     * - Lọc bỏ phần tử null trong các list filter trước khi gọi repository.
     * - Nếu job/location trống -> truyền null xuống repo (repo nên xử lý IS NULL như “bỏ lọc”).
     */
    public List<JobPostActivity> search(String job,
                                        String location,
                                        List<String> type,
                                        List<String> remote,
                                        LocalDate searchDate) {

        // Chuẩn hóa input
        String kw = StringUtils.hasText(job) ? job.trim() : null;
        String loc = StringUtils.hasText(location) ? location.trim() : null;

        List<String> types = (type == null) ? List.of() : filtered(type);
        List<String> remotes = (remote == null) ? List.of() : filtered(remote);

        // Gọi repo đúng chữ ký
        return (searchDate == null)
                ? jobPostActivityRepository.searchWithoutDate(kw, loc, remotes, types)
                : jobPostActivityRepository.search(kw, loc, remotes, types, searchDate);
    }

    private static List<String> filtered(List<String> in) {
        return in.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public List<RecruiterJobsDto> getRecruiterJobs(int recruiter) {
        List<IRecruiterJobs> rows = jobPostActivityRepository.getRecruiterJobs(recruiter);
        List<RecruiterJobsDto> out = new ArrayList<>();
        for (IRecruiterJobs rec : rows) {
            JobLocation loc = new JobLocation(rec.getLocationId(), rec.getCity(), rec.getState(), rec.getCountry());
            JobCompany comp = new JobCompany(rec.getCompanyId(), rec.getName(), "");
            out.add(new RecruiterJobsDto(
                    rec.getTotalCandidates(),
                    rec.getJob_post_id(),
                    rec.getJob_title(),
                    loc,
                    comp
            ));
        }
        return out;
    }

    /* ===================== UPDATE / DELETE ===================== */

    /**
     * Cập nhật job từ dữ liệu form.
     * - Kiểm tra quyền sở hữu (người đăng hiện tại).
     * - Chỉ ghi đè các field được phép sửa.
     */
    @Transactional
    public void updateFromForm(int id, JobPostActivity form) {
        JobPostActivity job = getOne(id);
        Users current = usersService.getCurrentUser();
        if (current == null || job.getPostedById() == null
                || !Objects.equals(job.getPostedById().getUserId(), current.getUserId())) {
            throw new SecurityException("Bạn không có quyền sửa job này");
        }

        // copy field cho phép sửa
        job.setJobTitle(form.getJobTitle());
        job.setJobType(form.getJobType());
        job.setRemote(form.getRemote());
        job.setSalary(form.getSalary());
        job.setDescriptionOfJob(form.getDescriptionOfJob());
        job.setJobLocationId(form.getJobLocationId());
        job.setJobCompanyId(form.getJobCompanyId());
        job.setExperienceRequired(form.getExperienceRequired());
        job.setCertificateRequired(form.getCertificateRequired());
        job.setField(form.getField());
        job.setNumber(form.getNumber());

        // Nếu dùng optimistic locking (@Version), đảm bảo form có field version hidden trong template.
        jobPostActivityRepository.save(job);
    }

    /**
     * Xóa job an toàn:
     * - Kiểm tra quyền sở hữu
     * - Xóa record con trước (Apply/Save) nếu chưa cấu hình cascade/orphanRemoval tại DB/Entity.
     */
    @Transactional
    public void delete(int id) {
        JobPostActivity job = getOne(id);
        Users current = usersService.getCurrentUser();
        if (current == null || job.getPostedById() == null
                || !Objects.equals(job.getPostedById().getUserId(), current.getUserId())) {
            throw new SecurityException("Bạn không có quyền xóa job này");
        }
        jobPostActivityRepository.delete(job); // DB đã bật FK ON DELETE CASCADE thì sẽ tự xóa apply/save
    }

    /**
     * Tìm kiếm chỉ theo thanh search (job + location) và gắn lại cờ isActive/isSaved
     * cho người dùng hiện tại (nếu là Job Seeker) để UI hiển thị “Đã ứng tuyển/Đã lưu”.
     */
    public List<JobPostActivity> searchOnly(String job, String location) {
        String j = (job == null || job.isBlank()) ? null : job.trim();
        String l = (location == null || location.isBlank()) ? null : location.trim();

        List<JobPostActivity> result = (j == null && l == null)
                ? jobPostActivityRepository.findAllByOrderByPostedDateDesc()
                : jobPostActivityRepository.searchByKeyword(j, l);

        // -> gắn lại cờ applied/saved theo user hiện tại
        return decorateWithUserFlags(result);
    }

    /* ===================== gắn cờ applied/saved ===================== */

    /**
     * Với danh sách job, set lại hai cờ tạm thời:
     *   - job.setIsActive(true) nếu user hiện tại đã apply job đó
     *   - job.setIsSaved(true)  nếu user hiện tại đã lưu job đó
     * Nếu user không phải Job Seeker (VD: Recruiter) thì bỏ qua.
     */
    private List<JobPostActivity> decorateWithUserFlags(List<JobPostActivity> jobs) {
        if (jobs == null || jobs.isEmpty()) return jobs;

        Object profile = usersService.getCurrentUserProfile();
        if (profile instanceof JobSeekerProfile jobSeeker) {
            // lấy danh sách apply & save của ứng viên hiện tại
            List<JobSeekerApply> applied = jobSeekerApplyRepository.findByUserId(jobSeeker);
            List<JobSeekerSave> saved = jobSeekerSaveRepository.findByUserId(jobSeeker);

            for (JobPostActivity job : jobs) {
                boolean isApplied = applied.stream()
                        .anyMatch(a -> a.getJob() != null
                                && a.getJob().getJobPostId() == job.getJobPostId());
                boolean isSaved = saved.stream()
                        .anyMatch(s -> s.getJob() != null
                                && s.getJob().getJobPostId() == job.getJobPostId());

                job.setIsActive(isApplied);
                job.setIsSaved(isSaved);
            }
        }
        // Recruiter hoặc chưa đăng nhập: không gắn cờ
        return jobs;
    }
}
