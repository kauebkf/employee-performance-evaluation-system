# Employee Performance Evaluation System

A Spring Boot microservice for managing employee performance reviews with MongoDB for storage and Kafka for event streaming.

## Features

- Submit and track employee performance reviews
- Calculate performance metrics and trends
- Generate peer comparison reports
- Produce department-level summaries
- Event-driven architecture using Kafka
- Data persistence with MongoDB

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher
- Docker and Docker Compose
- MongoDB (provided via Docker)
- Apache Kafka (provided via Docker)

## Quick Start with Docker

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/employee-performance-evaluation-system.git
   cd employee-performance-evaluation-system
   ```

2. Build and start the application using Docker Compose:
   ```bash
   docker compose up --build
   ```

   This will:
   - Build the Spring Boot application
   - Start MongoDB
   - Start Kafka and Zookeeper
   - Start the application container

The application will be available at `http://localhost:8080`

## Development Setup

1. Build the application:
   ```bash
   mvn clean install
   ```

2. Run tests (includes unit tests and integration tests):
   ```bash
   mvn verify
   ```

3. Start the application locally:
   ```bash
   mvn spring-boot:run
   ```

## API Endpoints

### Performance Reviews
- `POST /reviews` - Submit a new performance review
- `GET /employees/{employeeId}/performance` - Get employee performance report
- `GET /employees/{employeeId}/peer-comparison` - Get peer comparison report
- `GET /departments/{departmentId}/performance-summary` - Get department performance summary

### Example Request

```json
POST /reviews
{
  "employeeId": "emp123",
  "reviewerId": "rev456",
  "employeeInfo": {
    "departmentId": "dev_dept",
    "role": "software_engineer"
  },
  "metrics": {
    "goalAchievement": 85,
    "skillLevel": 90,
    "teamwork": 95
  },
  "comments": "Excellent performance in project deliveries"
}
```

## Configuration

Key application properties (in `application.properties`):
- MongoDB connection settings
- Kafka broker configuration
- Application-specific settings

## Testing

The project includes:
- Unit tests
- Integration tests (with embedded MongoDB)
- Kafka consumer tests

Run all tests with:
```bash
mvn verify
```

### Loading Test Data via Kafka

The project includes a PowerShell script to load sample performance reviews through Kafka:

1. Ensure the application is running with Docker Compose:
   ```bash
   docker compose up --build
   ```

2. Review the sample data in `test-reviews.json`. This file contains example performance reviews with different metrics and roles.

3. Run the PowerShell script to send reviews to Kafka:
   ```powershell
   ./send-reviews.ps1
   ```

   This script will:
   - Read performance reviews from `test-reviews.json`
   - Send each review to the Kafka topic `performance-reviews`
   - Wait 1 second between reviews to ensure proper processing
   - Display a confirmation message for each sent review

4. The application's Kafka consumer will automatically process these reviews and store them in MongoDB.

5. You can then verify the data by calling the API endpoints:
   ```bash
   # Get performance report for an employee
   curl http://localhost:8080/employees/emp101/performance

   # Get department summary
   curl http://localhost:8080/departments/eng001/summary
   ```

## Docker Commands

- Build and start all services:
  ```bash
  docker compose up --build
  ```

- Start in detached mode:
  ```bash
  docker compose up -d
  ```

- Stop all services:
  ```bash
  docker compose down
  ```

- View logs:
  ```bash
  docker compose logs -f
  ```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
