package com.result.bputresultextract.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvResultRow {
    private String regdNo;
    private String semId;
    private String subjectCode;
    private Integer credits;
    private String grade;
    private String examSession;
}
