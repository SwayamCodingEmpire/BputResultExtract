package com.result.bputresultextract.controller;

import com.result.bputresultextract.dto.CsvResultRow;
import com.result.bputresultextract.dto.ExtractionRequest;
import com.result.bputresultextract.service.ResultExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Result Extraction", description = "APIs for extracting BPUT student results")
public class ResultExtractionController {

    private final ResultExtractionService resultExtractionService;

    @Operation(
            summary = "Extract student results",
            description = "Extracts student result data from BPUT for multiple registration numbers across a range of academic sessions. " +
                    "The API processes each registration number, fetches semester lists and subject details from BPUT servers, " +
                    "and returns the aggregated data in CSV format. Supports concurrent processing with rate limiting."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully extracted results and returned CSV file",
                    content = @Content(
                            mediaType = "text/csv",
                            schema = @Schema(type = "string"),
                            examples = @ExampleObject(
                                    value = "regdNo,semId,subjectCode,credits,grade,examSession\n" +
                                            "2101289370,3,RCS3C001,3,A,Odd-(2022-23)\n" +
                                            "2101289370,3,RCS3C002,3,A,Odd-(2022-23)"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - missing required fields",
                    content = @Content(mediaType = "text/plain")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during extraction",
                    content = @Content(mediaType = "text/plain")
            )
    })
    @PostMapping(value = "/extract", produces = "text/csv")
    public ResponseEntity<String> extractResults(
            @Parameter(
                    description = "Extraction request containing registration numbers, sessions, and date of birth",
                    required = true,
                    schema = @Schema(implementation = ExtractionRequest.class),
                    example = """
                            {
                              "startRegNo": "2101289370",
                              "endRegNo": "2101289380",
                              "startSession": "Odd-(2022-23)",
                              "endSession": "Even-(2024-25)",
                              "dob": "2001-03-13"
                            }
                            """
            )
            @RequestBody ExtractionRequest request) {
        log.info("Received extraction request from regNo {} to {}", request.getStartRegNo(), request.getEndRegNo());

        try {
            // Validate request
            if (request.getStartRegNo() == null || request.getStartRegNo().isEmpty()) {
                return ResponseEntity.badRequest().body("startRegNo is required");
            }
            if (request.getEndRegNo() == null || request.getEndRegNo().isEmpty()) {
                return ResponseEntity.badRequest().body("endRegNo is required");
            }
            if (request.getDob() == null || request.getDob().isEmpty()) {
                return ResponseEntity.badRequest().body("dob is required");
            }
            if (request.getStartSession() == null || request.getStartSession().isEmpty()) {
                return ResponseEntity.badRequest().body("startSession is required");
            }

            // Generate list of registration numbers
            List<String> regnos = generateRegNoList(request.getStartRegNo(), request.getEndRegNo());
            log.info("Generated {} registration numbers to process", regnos.size());

            // Extract results
            List<CsvResultRow> results = resultExtractionService.extractResultsForRegnos(regnos, request.getDob(),
                    request.getStartSession(), request.getEndSession());

            // Generate CSV
            String csv = resultExtractionService.generateCsv(results);

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("bput_results_%s.csv", timestamp);

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("text", "csv"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            log.info("Extraction completed. Total records: {}", results.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csv);

        } catch (Exception e) {
            log.error("Error during extraction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error extracting results: " + e.getMessage());
        }
    }

    @Operation(
            summary = "Health check",
            description = "Check if the Result Extraction Service is running and healthy"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy and running",
            content = @Content(mediaType = "text/plain")
    )
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Result Extraction Service is running");
    }

    /**
     * Generate a list of registration numbers from startRegNo to endRegNo (inclusive)
     * Example: generateRegNoList("2101289370", "2101289373") -> ["2101289370", "2101289371", "2101289372", "2101289373"]
     */
    private List<String> generateRegNoList(String startRegNo, String endRegNo) {
        List<String> regnos = new ArrayList<>();

        try {
            long start = Long.parseLong(startRegNo);
            long end = Long.parseLong(endRegNo);

            if (start > end) {
                throw new IllegalArgumentException("startRegNo must be less than or equal to endRegNo");
            }

            // Safety check to prevent generating too many registration numbers
            long count = end - start + 1;
            if (count > 10000) {
                throw new IllegalArgumentException("Cannot generate more than 10000 registration numbers at once. " +
                        "Current range would generate " + count + " numbers.");
            }

            for (long i = start; i <= end; i++) {
                regnos.add(String.valueOf(i));
            }

            log.info("Generated {} registration numbers from {} to {}", regnos.size(), startRegNo, endRegNo);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Registration numbers must be numeric. " +
                    "Invalid format: startRegNo=" + startRegNo + ", endRegNo=" + endRegNo);
        }

        return regnos;
    }
}
