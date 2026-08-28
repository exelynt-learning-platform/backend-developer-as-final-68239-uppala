# Resource Booking System

A secure, enterprise-ready RESTful API for a Resource Booking System built with Spring Boot, Java 17+, MySQL/PostgreSQL, and Spring Security (JWT).

## Features

- **Authentication**: Stateless JWT-based login (`/api/auth/login`) with BCrypt password hashing.
- **Role-Based Access Control (RBAC)**:
  - `ADMIN`: Full CRUD access to Resources and Reservations.
  - `USER`: Read-only access to Resources; Create and View *own* Reservations only.
- **Resource Management**: Manage bookable items like rooms and equipment.
- **Reservation Management**: Users can book resources with specific start/end times and validated decimal prices. Status tracking (`PENDING`, `CONFIRMED`, `CANCELLED`).
- **Advanced Querying**: Dynamic Filtering (status, min/max price), Pagination, and Multi-field Sorting.
- **API Documentation**: Interactive Swagger/OpenAPI UI integration.
- **Testing**: Comprehensive Unit and Integration test coverage for business logic and security filters.

## Technologies Used
- Spring Boot 3+ (Java 17+)
- Spring Security 6 & JWT (`jjwt 0.11.5`)
- Spring Data JPA & Hibernate
- MySQL / PostgreSQL
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 & Mockito

## Configuration & Environment Variables

The application can be configured via environment variables or local configuration profiles. All credentials are fully externalized:

| Variable | Description | Default / Example |
|---|---|---|
| `DB_URL` | JDBC Database Connection URL | `jdbc:mysql://localhost:3306/booking_db?createDatabaseIfNotExist=true` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | *(Set securely via ENV)* |
| `JWT_SECRET` | 256-bit Hex secret key for signing JWTs | *(Set securely via ENV)* |
| `JWT_EXPIRATION_MS`| JWT validity duration in milliseconds | `86400000` (24 Hours) |
| `SERVER_PORT` | Application server port | `8081` |
| `ADMIN_SEED_PASSWORD` | Initial password for the admin seed account | *(Configurable via ENV)* |
| `USER_SEED_PASSWORD` | Initial password for the standard user seed account | *(Configurable via ENV)* |

## API endpoints

All endpoints other than login require a Bearer token. `USER` accounts can read resources and create/view only their own reservations; `ADMIN` accounts can manage resources and reservations.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Authenticate and obtain a JWT. |
| `GET` | `/api/resources` | USER, ADMIN | List resources. |
| `GET` | `/api/resources/{id}` | USER, ADMIN | Get one resource. |
| `POST` | `/api/resources` | ADMIN | Create a resource. |
| `PUT` | `/api/resources/{id}` | ADMIN | Update a resource. |
| `DELETE` | `/api/resources/{id}` | ADMIN | Delete a resource. |
| `GET` | `/api/reservations` | USER, ADMIN | List reservations (users see only their own). Supports `status`, `minPrice`, `maxPrice`, `page`, `size`, `sortBy`, and `sortDir`. |
| `GET` | `/api/reservations/{id}` | USER, ADMIN | Get a reservation (users must own it). |
| `POST` | `/api/reservations` | USER, ADMIN | Create a pending reservation. Price must be positive, have at most two decimal places, and fit `DECIMAL(10,2)`. |
| `PUT` | `/api/reservations/{id}/status?status=PENDING|CONFIRMED|CANCELLED` | ADMIN | Update reservation status. |
| `DELETE` | `/api/reservations/{id}` | ADMIN | Delete a reservation. |

## Setup & Running Locally

### 1. Database Setup
Create the database schema in your local MySQL or PostgreSQL instance:

```sql
CREATE DATABASE booking_db;
```

### 2. Build and Run
Run the application using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

Or build the production package and execute the JAR:
```bash
./mvnw clean package
java -jar target/resource-booking-0.0.1-SNAPSHOT.jar
```

### 3. Automated Tests
Run the comprehensive test suite (including authorization integration tests):

```bash
./mvnw clean test
```

### 4. API Documentation
Navigate to the Swagger UI in your browser:
[http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
