package com.luv2code.jobportal.controller;

import com.luv2code.jobportal.entity.RecruiterProfile;
import com.luv2code.jobportal.repository.JobPostActivityRepository;
import com.luv2code.jobportal.repository.JobSeekerApplyRepository;
import com.luv2code.jobportal.repository.JobSeekerProfileRepository;
import com.luv2code.jobportal.repository.RecruiterProfileRepository;
import com.luv2code.jobportal.services.UsersTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;
    @Autowired
    private JobPostActivityRepository jobPostActivityRepository;
    @Autowired
    private JobSeekerProfileRepository jobSeekerProfileRepository;
    @Autowired
    private JobSeekerApplyRepository jobSeekerApplyRepository;


    @GetMapping("/")
    public String home(Model model) {
        Long totalJobs = jobPostActivityRepository.count();
        Long totalRecruiter = recruiterProfileRepository.count();
        Long totalJobseeker = jobSeekerProfileRepository.count();
        Long totalJobseekerApply = jobSeekerApplyRepository.count();
        Long totalJobToday = jobPostActivityRepository.countTodayJobPosts();
        Long totalApplied = jobSeekerApplyRepository.count();
        model.addAttribute("totalApplied", totalApplied);
        model.addAttribute("totalJobToday", totalJobToday);
        model.addAttribute("totalJobseekerApply", totalJobseekerApply);
        model.addAttribute("totalJobseeker", totalJobseeker);
        model.addAttribute("totalJobs", totalJobs);
        model.addAttribute("totalRecruiter", totalRecruiter);
        return "index";
    }
}
