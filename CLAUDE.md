# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MARUNI** is a Spring Boot application built with Java 21, implementing JWT-based authentication, Redis caching, OAuth2 integration, and comprehensive API documentation with Swagger. The project follows a global exception handling pattern with standardized API responses.

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application (development profile active by default)
./gradlew bootRun

# Clean build
./gradlew clean build
```

### Testing
```bash
# Run all tests
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Database
- **Database**: PostgreSQL (runtime dependency)
- **Configuration**: Environment-based configuration through `.env` file
- **Required Environment Variables**:
  - `JWT_SECRET_KEY`
  - `JWT_ACCESS_EXPIRATION` (default: 3600000ms = 1 hour)
  - `JWT_REFRESH_EXPIRATION` (default: 86400000ms = 24 hours)

## Architecture

### Package Structure
```
com.anyang.maruni/
├── global/                    # Cross-cutting concerns
│   ├── config/               # Configuration classes
│   │   ├── SwaggerConfig     # OpenAPI/Swagger configuration
│   │   ├── SecurityConfig    # Spring Security setup
│   │   └── RedisConfig       # Redis connection configuration
│   ├── response/             # Standardized API response system
│   │   ├── ApiResponseAdvice # Automatic response wrapping
│   │   ├── AutoApiResponse   # Annotation for response wrapping
│   │   └── dto/CommonApiResponse # Standard response DTO
│   ├── exception/            # Global exception handling
│   │   ├── BaseException     # Custom base exception
│   │   └── GlobalExceptionHandler # Centralized exception handling
│   └── swagger/              # Swagger customization
└── MaruniApplication         # Main application class
```

### Key Architectural Components

#### 1. Global Response System
- **`@AutoApiResponse`**: Annotation to enable automatic response wrapping
- **`ApiResponseAdvice`**: Intercepts controller responses and wraps them in `CommonApiResponse`
- **`@SuccessCode`**: Annotation to specify custom success codes
- **Response Format**: All API responses follow the standardized `CommonApiResponse<T>` structure

#### 2. Exception Handling
- **`GlobalExceptionHandler`**: Centralized exception handling with `@RestControllerAdvice`
- **`BaseException`**: Custom exception base class with error code integration
- **Error Codes**: Enum-based error code system with HTTP status mapping
- **Validation Errors**: Automatic handling of `@Valid` annotation failures with detailed field error reporting

#### 3. Security & Authentication
- **JWT-based authentication** with access and refresh tokens
- **OAuth2 client support** for social login integration
- **Public URLs**: Configured in `application.yml` under `security.public-urls`
- **Admin URLs**: Protected admin endpoints under `security.admin-urls`

#### 4. API Documentation
- **Swagger UI**: Available at `/api-docs`
- **Custom Exception Documentation**: Automatic error response examples via `@CustomExceptionDescription`
- **JWT Security Scheme**: Bearer token authentication in Swagger UI
- **Operation Customization**: Automatic security requirements for `@PreAuthorize` annotated methods

## Development Notes

### Spring Profiles
- **Default Profile**: `dev` (set in `application.yml`)
- **Configuration**: Uses Spring Boot's external configuration with `.env` file support

### Database Auditing
- **JPA Auditing**: Enabled via `@EnableJpaAuditing` in main application class
- **Base Entity**: `BaseTimeEntity` for automatic timestamp management

### Redis Integration
- **Spring Data Redis**: Configured with connection pooling via `commons-pool2`
- **Usage**: Suitable for caching, session management, and token storage

### Code Conventions
- **Lombok**: Used extensively for reducing boilerplate code
- **Validation**: Bean Validation with Hibernate Validator
- **Exception Naming**: Custom exceptions extend `BaseException`
- **Response Wrapping**: Use `@AutoApiResponse` on controllers or methods that need standardized responses