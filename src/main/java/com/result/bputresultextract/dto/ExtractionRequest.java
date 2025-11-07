package com.result.bputresultextract.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for extracting BPUT student results")
public class ExtractionRequest {

    @Schema(
            description = "Starting student registration number. All registration numbers from startRegNo to endRegNo will be processed.",
            example = "2101289370",
            required = true
    )
    private String startRegNo;

    @Schema(
            description = "Ending student registration number (inclusive). Must be greater than or equal to startRegNo.",
            example = "2101289380",
            required = true
    )
    private String endRegNo;

    @Schema(
            description = "Starting academic session (format: Odd-(YYYY-YY) or Even-(YYYY-YY))",
            example = "Odd-(2022-23)",
            required = true
    )
    private String startSession;

    @Schema(
            description = "Ending academic session (format: Odd-(YYYY-YY) or Even-(YYYY-YY)). " +
                    "All sessions from startSession to endSession (inclusive) will be processed.",
            example = "Even-(2024-25)",
            required = true
    )
    private String endSession;

    @Schema(
            description = "Student date of birth in YYYY-MM-DD format",
            example = "2001-03-13",
            required = true,
            pattern = "^\\d{4}-\\d{2}-\\d{2}$"
    )
    private String dob;
}
