# BPUT Result Extraction API - Usage Guide

## Overview
This API extracts student result data from BPUT (Biju Patnaik University of Technology) for multiple registration numbers across multiple academic sessions and returns the data in CSV format.

## Interactive API Documentation

Once the application is running, you can access the **Swagger UI** (OpenAPI documentation) at:

**🔗 http://localhost:8080/swagger-ui.html**

This provides:
- Interactive API documentation
- Try-it-out functionality to test endpoints directly from the browser
- Request/response examples
- Schema definitions

You can also access the raw OpenAPI JSON specification at:
- **http://localhost:8080/api-docs**

## Endpoint

### POST `/api/results/extract`

Extracts student results for specified registration numbers across a range of academic sessions.

**Content-Type:** `application/json`
**Response Type:** `text/csv`

## Request Body

```json
{
  "startRegNo": "2101289370",
  "endRegNo": "2101289380",
  "startSession": "Odd-(2022-23)",
  "endSession": "Even-(2024-25)",
  "dob": "2001-03-13"
}
```

### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `startRegNo` | String | Yes | Starting registration number (e.g., `2101289370`) |
| `endRegNo` | String | Yes | Ending registration number (inclusive, e.g., `2101289380`). All registration numbers from startRegNo to endRegNo will be processed. |
| `startSession` | String | Yes | Starting academic session (format: `Odd-(YYYY-YY)` or `Even-(YYYY-YY)`) |
| `endSession` | String | Yes | Ending academic session (format: `Odd-(YYYY-YY)` or `Even-(YYYY-YY)`) |
| `dob` | String | Yes | Date of birth in format `YYYY-MM-DD` (e.g., `2001-03-13`) |

### Session Format
Sessions follow this pattern:
- **Odd-(2022-23)** → **Even-(2022-23)** → **Odd-(2023-24)** → **Even-(2023-24)** → etc.

The API will process all sessions from `startSession` to `endSession` (inclusive).

## Response

The API returns a CSV file with the following columns:

| Column | Description |
|--------|-------------|
| `regdNo` | Registration number |
| `semId` | Semester ID |
| `subjectCode` | Subject code |
| `credits` | Credit points for the subject |
| `grade` | Grade obtained (e.g., O, A, B, C, etc.) |
| `examSession` | Examination session |

### Sample CSV Output

```csv
regdNo,semId,subjectCode,credits,grade,examSession
2101289370,3,RCS3C001,3,A,Odd-(2022-23)
2101289370,3,RCS3C002,3,A,Odd-(2022-23)
2101289370,3,REN3E001,3,O,Odd-(2022-23)
```

## How It Works

The API processes requests in the following steps:
1. **Generates registration numbers**: Creates a list of all registration numbers from startRegNo to endRegNo (e.g., 2101289370 to 2101289380 generates 11 numbers)
2. **For each registration number and each session**:
   - Calls BPUT API to get list of semesters: `https://results.bput.ac.in/student-results-list`
   - For each semester, calls BPUT API to get subject details: `https://results.bput.ac.in/student-results-subjects-list`
3. Extracts required fields (regdNo, semId, subjectCode, credits, grade, examSession) and aggregates all results
4. Returns data as downloadable CSV file

**Example**: If you provide startRegNo=2101289370, endRegNo=2101289372, and 2 sessions, the API will process:
- 3 registration numbers × 2 sessions = 6 total API call sequences

## Features

- **Concurrent Processing**: Uses Java 21 Virtual Threads to process multiple requests simultaneously
- **Rate Limiting**: Limits concurrent requests to 5 at a time to avoid overwhelming the BPUT server
- **Retry Mechanism**: Automatically retries failed requests up to 3 times with exponential backoff
- **Error Handling**: Continues processing even if some requests fail
- **CSV Export**: Returns results in CSV format ready for download
- **CORS Enabled**: Configured to allow cross-origin requests from browser clients

## Example Usage

### Using cURL

```bash
curl -X POST http://localhost:8080/api/results/extract \
  -H "Content-Type: application/json" \
  -d '{
    "startRegNo": "2101289370",
    "endRegNo": "2101289380",
    "startSession": "Odd-(2022-23)",
    "endSession": "Even-(2024-25)",
    "dob": "2001-03-13"
  }' \
  --output results.csv
```

### Using HTTPie

```bash
http POST http://localhost:8080/api/results/extract \
  startRegNo="2101289370" \
  endRegNo="2101289380" \
  startSession="Odd-(2022-23)" \
  endSession="Even-(2024-25)" \
  dob="2001-03-13" \
  > results.csv
```

### Using Postman

1. Create a POST request to `http://localhost:8080/api/results/extract`
2. Set Headers: `Content-Type: application/json`
3. Set Body (raw JSON):
```json
{
  "startRegNo": "2101289370",
  "endRegNo": "2101289380",
  "startSession": "Odd-(2022-23)",
  "endSession": "Even-(2024-25)",
  "dob": "2001-03-13"
}
```
4. Click "Send"
5. Save the response as a CSV file

### Using JavaScript/Fetch (Browser)

```javascript
fetch('http://localhost:8080/api/results/extract', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    startRegNo: '2101289370',
    endRegNo: '2101289380',
    startSession: 'Odd-(2022-23)',
    endSession: 'Even-(2024-25)',
    dob: '2001-03-13'
  })
})
.then(response => response.blob())
.then(blob => {
  // Create download link
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'bput_results.csv';
  document.body.appendChild(a);
  a.click();
  a.remove();
})
.catch(error => console.error('Error:', error));
```

### Using Axios (JavaScript)

```javascript
const axios = require('axios');

axios.post('http://localhost:8080/api/results/extract', {
  startRegNo: '2101289370',
  endRegNo: '2101289380',
  startSession: 'Odd-(2022-23)',
  endSession: 'Even-(2024-25)',
  dob: '2001-03-13'
}, {
  responseType: 'blob'
})
.then(response => {
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', 'bput_results.csv');
  document.body.appendChild(link);
  link.click();
  link.remove();
})
.catch(error => console.error('Error:', error));
```

## Health Check

### GET `/api/results/health`

Returns service status.

**Response:** `Result Extraction Service is running`

## Running the Application

```bash
# Using Maven Wrapper
./mvnw spring-boot:run

# Or build and run the WAR
./mvnw clean package
java -jar target/BputResultExtract-0.0.1-SNAPSHOT.war
```

The application runs on port **8080** by default.

## Error Handling

- If any registration number fails, the API continues processing other registration numbers
- Failed requests are automatically retried up to 3 times with 2-second exponential backoff
- Errors are logged but don't stop the entire extraction process
- Empty results are handled gracefully

## Performance Considerations

- Processing time depends on:
  - Range of registration numbers (endRegNo - startRegNo)
  - Number of sessions to process
  - Response time of BPUT server
  - Number of semesters per student

- Virtual threads allow efficient handling of I/O-bound operations
- Rate limiting prevents overwhelming the BPUT server (max 5 concurrent requests)
- **Safety limit**: Maximum 10,000 registration numbers per request (e.g., from 2101289370 to 2101299369)
- Recommended: Keep ranges reasonable (e.g., 100-500 students at a time) for better performance

## CORS Configuration

The API is configured with CORS (Cross-Origin Resource Sharing) enabled, allowing browser-based clients to make requests from any origin.

**Current Configuration (Development):**
- Allows all origins (`*`)
- Allows all standard HTTP methods (GET, POST, PUT, DELETE, OPTIONS, PATCH)
- Allows credentials
- Exposes necessary headers for file downloads

**Production Recommendations:**
For production deployments, update `src/main/java/com/result/bputresultextract/config/CorsConfig.java` to restrict allowed origins:

```java
config.setAllowedOrigins(Arrays.asList(
    "https://yourdomain.com",
    "https://www.yourdomain.com"
));
```

## Notes

- The BPUT server can be slow and unreliable at times
- The API includes retry logic to handle intermittent failures
- All dates of birth must be in `YYYY-MM-DD` format
- Session formats must match exactly: `Odd-(YYYY-YY)` or `Even-(YYYY-YY)`
- CORS is enabled for browser-based clients - configure origins appropriately for production
