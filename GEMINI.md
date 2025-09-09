# Gemini Project Context: MARUNI

## Project Overview

This is a Java Spring Boot project for a service named **MARUNI**. The project is built with Java 21 and Spring Boot 3.5.3. It utilizes several key technologies:

*   **Spring Boot:** For building the web application.
*   **Spring Data JPA:** For database interaction and Object-Relational Mapping (ORM).
*   **Spring Security:** For authentication and authorization.
*   **Redis:** For caching and session management.
*   **PostgreSQL:** As the primary database.
*   **Swagger (OpenAPI):** For API documentation.
*   **Gradle:** As the build tool.

The project follows a standard Spring Boot project structure with a base package of `com.anyang.maruni`. The codebase is organized into the following main packages:

*   `com.anyang.maruni.domain`: Intended for core business logic, although currently empty.
*   `com.anyang.maruni.global`: Contains global configurations, exception handling, entities, and other cross-cutting concerns.

## Building and Running

### Building the Project

To build the project and its dependencies, run the following command in the project's root directory:

```bash
./gradlew build
```

### Running the Project

To run the application, use the following command:

```bash
./gradlew bootRun
```

The application will start on the port configured in `application.properties` or `application.yml` (by default, it's 8080).

## Development Conventions

*   **Package Structure:** The project follows a feature-oriented package structure within the `domain` package (although it is currently empty). Global configurations and common classes are placed in the `global` package.
*   **API Documentation:** The project uses Swagger (OpenAPI) for API documentation. Once the application is running, you can access the API documentation at `/swagger-ui.html`.
*   **Testing:** The project is set up for testing with JUnit 5. Tests are located in the `src/test/java` directory.
