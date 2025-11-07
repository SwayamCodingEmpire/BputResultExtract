# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BputResultExtract is a Java 21 Spring Boot 3.5.7 application packaged as a WAR file. The project appears to be designed for extracting and processing BPUT (likely university) result data. Currently, it's a skeleton project with Spring Web and WebFlux dependencies configured but no domain logic implemented yet.

## Prerequisites

- Java 21 JDK
- Maven 3.9.11+ (or use the included Maven Wrapper `./mvnw`)

## Build and Run Commands

### Building the Project
```bash
./mvnw clean package              # Build WAR file (target/BputResultExtract-0.0.1-SNAPSHOT.war)
./mvnw clean install              # Build and install to local Maven repository
```

### Running the Application
```bash
./mvnw spring-boot:run            # Run in development mode (port 8080 by default)
java -jar target/BputResultExtract-0.0.1-SNAPSHOT.war   # Run the built WAR
```

### Testing
```bash
./mvnw test                       # Run all tests
./mvnw test -Dtest=ClassName      # Run specific test class
./mvnw test -Dtest=ClassName#methodName  # Run specific test method
```

### Other Useful Commands
```bash
./mvnw dependency:tree            # View dependency hierarchy
./mvnw clean                      # Clean build artifacts
```

## Architecture

### Technology Stack
- **Framework**: Spring Boot 3.5.7 (servlet + reactive support)
- **Web Layer**: Spring Web MVC + Spring WebFlux
- **Build Tool**: Maven with Wrapper
- **Code Generation**: Lombok for reducing boilerplate
- **Testing**: JUnit 5 + Spring Boot Test + Reactor Test
- **Packaging**: WAR (supports both embedded and external servlet containers)

### Project Structure
```
src/main/java/com/result/bputresultextract/
├── BputResultExtractApplication.java    # Main Spring Boot application entry point
└── ServletInitializer.java              # WAR deployment configuration for external containers

src/main/resources/
├── application.properties               # Application configuration (minimal)
├── static/                              # Static web assets
└── templates/                           # Server-side templates
```

### Current State
This is a scaffold project with only the Spring Boot application class and servlet initializer. No controllers, services, repositories, or domain models are implemented yet. The base package `com.result.bputresultextract` is where all application code should reside.

### Expected Architecture Pattern
Based on the Spring Boot setup, the typical layered architecture would be:
- **Controllers** (`@RestController` or `@Controller`) - Handle HTTP requests
- **Services** (`@Service`) - Business logic
- **Repositories** (`@Repository`) - Data access layer (not yet configured with database)
- **DTOs/Models** - Data transfer objects and domain models
- **Configuration** (`@Configuration`) - Application configuration classes

### Dual Web Stack
The project includes both Spring Web (servlet-based) and Spring WebFlux (reactive). This allows for:
- Traditional blocking REST endpoints using Spring MVC
- Non-blocking reactive endpoints using WebFlux
- Consider the architecture carefully when implementing - mixing both patterns requires understanding their implications

## Development Notes

### Lombok Usage
Lombok is configured with annotation processing in the Maven compiler plugin. Use Lombok annotations (`@Data`, `@Builder`, `@Slf4j`, etc.) to reduce boilerplate code. Ensure your IDE has Lombok plugin installed.

### Deployment Options
1. **Embedded Server**: Run as standalone application using `java -jar` (Tomcat embedded)
2. **External Container**: Deploy the WAR to external Tomcat or other servlet containers via `ServletInitializer`

### Configuration
- Application name: `BputResultExtract` (defined in application.properties)
- Default port: 8080 (Spring Boot default)
- No active profiles or database configuration present
- Customize via `src/main/resources/application.properties` or `application.yml`
