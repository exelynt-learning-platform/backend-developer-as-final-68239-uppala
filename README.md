# Resource Booking System

A secure RESTful API for a Resource Booking System built with Spring Boot 3, Java 17, PostgreSQL, and Spring Security (JWT).

## Features

- **Authentication**: JWT-based login (`/api/auth/login`).
- **Role-Based Access Control**:
  - `ADMIN`: Full CRUD access to Resources and Reservations.
  - `USER`: Read-only access to Resources; Create and View *own* Reservations only.
- **Resource Management**: Manage bookable items like rooms and equipment.
- **Reservation Management**: Users can book resources with specific start/end times and prices. Status tracking (PENDING, CONFIRMED, CANCELLED).
- **Advanced Querying**: Pagination, Sorting, and Filtering (by status, price) for reservations.
- **API Documentation**: Integrated with Swagger/OpenAPI.

## Technologies Used
- Spring Boot 3.2.x
- Spring Security & JWT (jjwt)
- Spring Data JPA
- PostgreSQL
- SpringDoc OpenAPI (Swagger UI)
- Lombok

## Setup Instructions

### 1. Database Setup
Ensure you have PostgreSQL installed and running. Create a database named `booking_db`.

```sql
CREATE DATABASE booking_db;
```

Update `src/main/resources/application.yml` with your PostgreSQL username and password if they differ from the defaults:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booking_db
    username: postgres
    password: password
```

### 2. Build and Run
You can run the application using Maven:

```bash
./mvnw spring-boot:run
```

Or build the jar and run:
```bash
./mvnw clean package
java -jar target/resource-booking-0.0.1-SNAPSHOT.jar
```

### 3. Seed Data
On startup, the application will automatically create the schema (using `spring.jpa.hibernate.ddl-auto=update`) and populate some seed data:

**Users:**
- `username`: admin, `password`: admin123 (Role: ADMIN)
- `username`: user, `password`: user123 (Role: USER)

**Resources:**
- Conference Room A
- Projector Model X

### 4. API Documentation
Once the application is running, navigate to the Swagger UI to view and test all endpoints:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**Note on testing with Swagger:**
1. Use the `/api/auth/login` endpoint to get a JWT token using seed user credentials.
2. Click the "Authorize" button at the top of the Swagger UI and enter `Bearer <your-token>`.
3. You can now access the secured endpoints according to the logged-in user's role.
