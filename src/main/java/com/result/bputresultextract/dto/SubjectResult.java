package com.result.bputresultextract.dto;

import lombok.Data;

@Data
public class SubjectResult {
    private String course;
    private String semId;
    private String branchName;
    private String rollNo;
    private String subjectCODE;
    private String subjectTP;
    private String subjectName;
    private Integer subjectCredits;
    private String grade;
    private Integer points;
    private Integer creditPoints;
    private Integer recheck;
}