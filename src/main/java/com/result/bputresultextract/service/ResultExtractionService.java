package com.result.bputresultextract.service;

import com.result.bputresultextract.dto.CsvResultRow;
import com.result.bputresultextract.dto.ExtractionRequest;
import com.result.bputresultextract.dto.ResultListItem;
import com.result.bputresultextract.dto.SubjectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResultExtractionService {

    private final WebClient bputWebClient;
    private static final int MAX_CONCURRENT_REQUESTS = 5;
    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(2);

    public List<CsvResultRow> extractResultsForRegnos(List<String> regnos, String dob, String startSession, String endSession) {
        Semaphore semaphore = new Semaphore(MAX_CONCURRENT_REQUESTS);

        // Generate list of all sessions from start to end
        List<String> sessions = generateSessions(startSession, endSession);
        log.info("Processing {} registration numbers across {} sessions", regnos.size(), sessions.size());

        // Use virtual thread executor (stable in Java 21)
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<CsvResultRow>>> futures = new ArrayList<>();

            for (String regno : regnos) {
                for (String session : sessions) {
                    Future<List<CsvResultRow>> future = executor.submit(() -> {
                        try {
                            semaphore.acquire();
                            try {
                                return processRegnoForSession(regno, dob, session);
                            } finally {
                                semaphore.release();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            log.error("Thread interrupted while processing regno: {} session: {}", regno, session, e);
                            return new ArrayList<>();
                        }
                    });
                    futures.add(future);
                }
            }

            // Collect results from all futures
            List<CsvResultRow> allResults = new ArrayList<>();
            for (Future<List<CsvResultRow>> future : futures) {
                try {
                    allResults.addAll(future.get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread interrupted while collecting results", e);
                } catch (ExecutionException e) {
                    log.error("Error during result extraction", e.getCause());
                }
            }

            return allResults;

        } catch (Exception e) {
            log.error("Error during result extraction", e);
            throw new RuntimeException("Failed to extract results", e);
        }
    }

    /**
     * Generate list of sessions from startSession to endSession
     * Format: Odd-(2022-23), Even-(2022-23), Odd-(2023-24), Even-(2023-24), etc.
     */
    private List<String> generateSessions(String startSession, String endSession) {
        List<String> sessions = new ArrayList<>();

        String currentSession = startSession;
        sessions.add(currentSession);

        while (!currentSession.equals(endSession)) {
            currentSession = getNextSession(currentSession);
            sessions.add(currentSession);

            // Safety check to prevent infinite loop
            if (sessions.size() > 100) {
                log.error("Too many sessions generated. Check startSession and endSession values.");
                break;
            }
        }

        return sessions;
    }

    /**
     * Get the next session
     * Odd-(2022-23) -> Even-(2022-23)
     * Even-(2022-23) -> Odd-(2023-24)
     */
    private String getNextSession(String currentSession) {
        // Parse session format: "Odd-(2022-23)" or "Even-(2022-23)"
        String[] parts = currentSession.split("-\\(");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid session format: " + currentSession);
        }

        String semester = parts[0]; // "Odd" or "Even"
        String yearPart = parts[1].replace(")", ""); // "2022-23"

        if ("Odd".equals(semester)) {
            // Odd -> Even (same year)
            return "Even-(" + yearPart + ")";
        } else {
            // Even -> Odd (next year)
            String[] years = yearPart.split("-");
            int startYear = Integer.parseInt(years[0]);
            int endYear = Integer.parseInt(years[1]);

            return String.format("Odd-(%d-%02d)", startYear + 1, endYear + 1);
        }
    }

    private List<CsvResultRow> processRegnoForSession(String regno, String dob, String session) {
        log.info("Processing regno: {} for session: {}", regno, session);
        List<CsvResultRow> results = new ArrayList<>();

        try {
            // Fetch the list of semesters for this regno
            List<ResultListItem> semesterList = fetchResultList(regno, dob, session);

            if (semesterList == null || semesterList.isEmpty()) {
                log.warn("No results found for regno: {}", regno);
                return results;
            }

            // Process each semester
            for (ResultListItem item : semesterList) {
                try {
                    List<SubjectResult> subjects = fetchSubjectResults(item.getSemId(), regno, item.getExamSession());

                    if (subjects != null) {
                        for (SubjectResult subject : subjects) {
                            CsvResultRow row = CsvResultRow.builder()
                                    .regdNo(regno)
                                    .semId(subject.getSemId())
                                    .subjectCode(subject.getSubjectCODE())
                                    .credits(subject.getSubjectCredits())
                                    .grade(subject.getGrade())
                                    .examSession(item.getExamSession())
                                    .build();
                            results.add(row);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing semester {} for regno {}", item.getSemId(), regno, e);
                }
            }

        } catch (Exception e) {
            log.error("Error processing regno: {}", regno, e);
        }

        return results;
    }

    private List<ResultListItem> fetchResultList(String rollNo, String dob, String session) {
        log.info("Fetching result list for rollNo: {}, dob: {}, session: {}", rollNo, dob, session);

        return bputWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/student-results-list")
                        .queryParam("rollNo", rollNo)
                        .queryParam("dob", dob)
                        .queryParam("session", session)
                        .build())
                .exchangeToMono(response -> {
                    log.info("Response status for rollNo {}: {}", rollNo, response.statusCode());

                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(new ParameterizedTypeReference<List<ResultListItem>>() {})
                                .doOnNext(list -> log.info("Successfully fetched {} semesters for rollNo: {}",
                                        list != null ? list.size() : 0, rollNo))
                                .doOnError(error -> log.error("Error parsing response body for rollNo: {}, error: {}",
                                        rollNo, error.getMessage()));
                    } else {
                        return response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Non-2xx response for rollNo: {}, status: {}, body: {}",
                                        rollNo, response.statusCode(), body))
                                .then(Mono.error(new RuntimeException("HTTP " + response.statusCode() + " for rollNo: " + rollNo)));
                    }
                })
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .doBeforeRetry(signal ->
                                log.warn("Retrying fetchResultList for rollNo: {} (attempt {}), error: {}",
                                        rollNo, signal.totalRetries() + 1, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Max retries exceeded for fetchResultList: rollNo={}, last error: {}",
                                    rollNo, retrySignal.failure().getMessage());
                            return new RuntimeException("Failed to fetch result list after " + MAX_RETRIES + " retries for rollNo: " + rollNo);
                        }))
                .onErrorResume(error -> {
                    log.error("Returning empty list due to error for rollNo: {}, error: {}", rollNo, error.getMessage());
                    return Mono.just(new ArrayList<>());
                })
                .block(Duration.ofSeconds(30));
    }

    private List<SubjectResult> fetchSubjectResults(String semId, String rollNo, String session) {
        log.info("Fetching subject results for rollNo: {}, semId: {}, session: {}", rollNo, semId, session);

        return bputWebClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/student-results-subjects-list")
                        .queryParam("semid", semId)
                        .queryParam("rollNo", rollNo)
                        .queryParam("session", session)
                        .build())
                .exchangeToMono(response -> {
                    log.info("Response status for rollNo {}, semId {}: {}", rollNo, semId, response.statusCode());

                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(new ParameterizedTypeReference<List<SubjectResult>>() {})
                                .doOnNext(list -> log.info("Successfully fetched {} subjects for rollNo: {}, semId: {}",
                                        list != null ? list.size() : 0, rollNo, semId))
                                .doOnError(error -> log.error("Error parsing response body for rollNo: {}, semId: {}, error: {}",
                                        rollNo, semId, error.getMessage()));
                    } else {
                        return response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Non-2xx response for rollNo: {}, semId: {}, status: {}, body: {}",
                                        rollNo, semId, response.statusCode(), body))
                                .then(Mono.error(new RuntimeException("HTTP " + response.statusCode() + " for rollNo: " + rollNo + ", semId: " + semId)));
                    }
                })
                .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                        .doBeforeRetry(signal ->
                                log.warn("Retrying fetchSubjectResults for rollNo: {}, semId: {} (attempt {}), error: {}",
                                        rollNo, semId, signal.totalRetries() + 1, signal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Max retries exceeded for fetchSubjectResults: rollNo={}, semId={}, last error: {}",
                                    rollNo, semId, retrySignal.failure().getMessage());
                            return new RuntimeException("Failed to fetch subject results after " + MAX_RETRIES + " retries for rollNo: " + rollNo + ", semId: " + semId);
                        }))
                .onErrorResume(error -> {
                    log.error("Returning empty list due to error for rollNo: {}, semId: {}, error: {}", rollNo, semId, error.getMessage());
                    return Mono.just(new ArrayList<>());
                })
                .block(Duration.ofSeconds(30));
    }

    public String generateCsv(List<CsvResultRow> results) {
        StringBuilder csv = new StringBuilder();

        // CSV Header
        csv.append("regdNo,semId,subjectCode,credits,grade,examSession\n");

        // CSV Rows
        for (CsvResultRow row : results) {
            csv.append(String.format("%s,%s,%s,%d,%s,%s\n",
                    escapeCsvValue(row.getRegdNo()),
                    escapeCsvValue(row.getSemId()),
                    escapeCsvValue(row.getSubjectCode()),
                    row.getCredits() != null ? row.getCredits() : 0,
                    escapeCsvValue(row.getGrade()),
                    escapeCsvValue(row.getExamSession())
            ));
        }

        return csv.toString();
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
