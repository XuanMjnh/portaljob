package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.*;
import com.luv2code.jobportal.services.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class JobSeekerSaveController {

    private final UsersService usersService;
    private final JobSeekerProfileService jobSeekerProfileService;
    private final JobPostActivityService jobPostActivityService;
    private final JobSeekerSaveService jobSeekerSaveService;
    private final JobSeekerApplyService jobSeekerApplyService;

    public JobSeekerSaveController(UsersService usersService,
                                   JobSeekerProfileService jobSeekerProfileService,
                                   JobPostActivityService jobPostActivityService,
                                   JobSeekerSaveService jobSeekerSaveService,
                                   JobSeekerApplyService jobSeekerApplyService) {
        this.usersService = usersService;
        this.jobSeekerProfileService = jobSeekerProfileService;
        this.jobPostActivityService = jobPostActivityService;
        this.jobSeekerSaveService = jobSeekerSaveService;
        this.jobSeekerApplyService = jobSeekerApplyService;
    }

    @PostMapping("job-details/save/{id}")
    public String save(@PathVariable("id") int id) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            String currentUsername = authentication.getName();
            Users user = usersService.findByEmail(currentUsername);
            Optional<JobSeekerProfile> seekerProfile = jobSeekerProfileService.getOne(user.getUserId());
            JobPostActivity jobPostActivity = jobPostActivityService.getOne(id);

            if (seekerProfile.isPresent() && jobPostActivity != null) {
                JobSeekerProfile profile = seekerProfile.get();

                //Kiểm tra nếu đã lưu job này rồi thì bỏ qua
                if (!jobSeekerSaveService.existsByUserAndJob(profile, jobPostActivity)) {
                    JobSeekerSave newSave = new JobSeekerSave();
                    newSave.setUserId(profile);
                    newSave.setJob(jobPostActivity);
                    jobSeekerSaveService.addNew(newSave);
                }
            } else {
                throw new RuntimeException("User not found");
            }
        }
        return "redirect:/job-details-apply/" + id;
    }

    @GetMapping("saved-jobs/")
    public String savedJobs(Model model) {
        List<JobPostActivity> jobPost = new ArrayList<>();
        Object currentUserProfile = usersService.getCurrentUserProfile();

        List<JobSeekerSave> jobSeekerSaveList = jobSeekerSaveService.getCandidatesJob((JobSeekerProfile) currentUserProfile);
        List<JobSeekerApply> jobSeekerApplyList = jobSeekerApplyService.getCandidatesJobs((JobSeekerProfile) currentUserProfile);

        for (JobSeekerSave jobSeekerSave : jobSeekerSaveList) {
            JobPostActivity job = jobSeekerSave.getJob();

            boolean exist = jobSeekerApplyList.stream()
                    .anyMatch(j -> Objects.equals(j.getJob().getJobPostId(), job.getJobPostId()));

            job.setIsSaved(true); // vì đang ở trang "saved jobs" mà
            job.setIsActive(exist); // xem người này đã apply chưa

            jobPost.add(job);
        }

        model.addAttribute("jobPost", jobPost);
        model.addAttribute("user", currentUserProfile);
        return "saved-jobs";
    }

}
