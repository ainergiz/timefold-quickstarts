# Hospital Employee Scheduling System

A sophisticated hospital employee scheduling application built with **Java**, **Quarkus**, and **Timefold Solver** that automatically generates optimal shift schedules for hospital staff while respecting complex medical scheduling constraints.

![Employee Scheduling Screenshot](./employee-scheduling-screenshot.png)

## 🏥 Overview

This application solves the complex problem of scheduling hospital employees across multiple shifts, workplaces, and work groups while adhering to Turkish hospital regulations and employee preferences. It uses constraint-based optimization to find the best possible schedule that minimizes violations of hard constraints (must-follow rules) and soft constraints (preferences).

## ✨ Key Features

### 🎯 **Smart Scheduling Engine**
- **Constraint-based optimization** using Timefold Solver
- **Multi-criteria decision making** with hard and soft constraints
- **Real-time schedule generation** with configurable time limits
- **Incremental solving** for continuous improvement

### 🏥 **Hospital-Specific Constraints**
- **NYDH (Turkish Hospital Seniority) quota management** across workplaces
- **Work group daily limits** to prevent overstaffing
- **Workplace qualification enforcement** based on employee groups
- **Rest period enforcement** (23+ hours after night shifts)
- **Official leave and excluded day handling**

### 👥 **Employee Management**
- **Individual shift quotas** (night shifts, weekend shifts)
- **Availability tracking** (official leave, excluded days, preferred days)
- **Seniority-based assignments** with experience distribution
- **Work group synchronization** for team coordination

### 🔧 **Advanced Configuration**
- **Dynamic constraint weight adjustment** via REST API
- **Runtime configuration changes** without code modification
- **Zero-config defaults** for immediate operation
- **Thread-safe multi-user support**

## 🚀 Quick Start

### Prerequisites

1. **Java 17+** and **Maven 3.6+**
   ```bash
   # Using SDKMAN (recommended)
   sdk install java 17.0.9-tem
   sdk install maven 3.9.5
   ```

2. **Verify installation**
   ```bash
   java --version
   mvn --version
   ```

### Running the Application

1. **Clone and navigate**
   ```bash
   git clone <repository-url>
   cd timefold-quickstarts/java/employee-scheduling
   ```

2. **Start in development mode**
   ```bash
   mvn quarkus:dev
   ```

3. **Access the application**
   - **Web UI**: http://localhost:8080
   - **API Documentation**: http://localhost:8080/q/swagger-ui
   - **Health Check**: http://localhost:8080/q/health

## 📊 API Endpoints

### Core Scheduling Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/hospital-schedule/solve` | POST | Start asynchronous solving |
| `/hospital-schedule/solve-and-wait` | POST | Solve and wait for completion |
| `/hospital-schedule/schedule-view` | GET | Get formatted schedule view |
| `/hospital-schedule/result/{problemId}` | GET | Get solution by problem ID |
| `/hospital-schedule/status` | GET | Check solver status |

### Configuration Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/hospital-schedule/constraints` | GET | Get all constraint configurations |
| `/hospital-schedule/constraints/active` | GET | Get active constraints summary |
| `/hospital-schedule/constraints/{constraintId}` | PUT | Update specific constraint |
| `/hospital-schedule/constraints/reset` | POST | Reset to default constraints |

### Data Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/hospital-schedule/test-data` | GET | Get data statistics |
/ `hospital-schedule/schedule-view` - main one to get the schedule

## 🏗️ Architecture

### Core Components

```
src/main/java/org/acme/employeescheduling/
├── domain/                    # Business entities
│   ├── HospitalSchedule.java  # Main solution class
│   ├── HospitalEmployee.java  # Employee entity
│   ├── HospitalShift.java     # Shift entity
│   └── ...                    # Configuration entities
├── solver/                    # Constraint definitions
│   └── HospitalSchedulingConstraintProvider.java
├── rest/                      # REST API endpoints
│   └── HospitalScheduleResource.java
├── data/                      # Data parsing
│   └── HospitalDataParser.java
└── config/                    # Configuration services
    ├── ConstraintConfigurationService.java
    └── NydhQuotaConfiguration.java
```

### Constraint System

The application implements a sophisticated constraint system with **hard constraints** (must-follow) and **soft constraints** (preferences):

#### Hard Constraints
- **No overlapping shifts** for the same employee
- **Minimum rest periods** (23+ hours after night shifts)
- **No consecutive night shifts**
- **Respect official leave days**
- **Workplace qualification enforcement**
- **Work group daily limits**
- **Individual shift quotas**

#### Soft Constraints
- **NYDH quota optimization** for balanced experience distribution
- **Preferred day assignments**
- **Work group synchronization**

## 🔧 Configuration

### Application Properties

Key configuration options in `src/main/resources/application.properties`:

```properties
# Solver timeout (60 seconds for complex schedules)
quarkus.timefold.solver.termination.spent-limit=60s

# Constraint stream implementation
quarkus.timefold.solver.constraint-stream-impl-type=BAVET

# Multi-threading (requires Enterprise Edition)
# quarkus.timefold.solver.move-thread-count=AUTO
```

### Constraint Weights

Default constraint weights are automatically configured for optimal hospital scheduling:

```java
// Hard constraints (high penalties)
"Overlapping shift": 100
"Workplace qualification": 200
"Work group daily limit": 150

// Soft constraints (preferences)
"NYDH quota limits": 50
"Prefer selected days": 5
```

## 🧪 Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=HospitalSchedulingConstraintProviderTest
```

### Test Data

The application includes comprehensive test data in `src/main/resources/dahiliye.postman.txt` with:
- **80+ employees** across different seniority groups
- **400+ shifts** spanning multiple workplaces
- **Real hospital constraints** and quotas

## 🚀 Deployment

### Production Build

```bash
# Package application
mvn clean package

# Run packaged application
java -jar target/quarkus-app/quarkus-run.jar
```

### Docker Deployment

```bash
# Build container image
mvn package -Dcontainer

# Run container
docker run -p 8080:8080 hospital-scheduling:latest
```

### Native Build (Optional)

For improved startup performance:

```bash
# Install GraalVM native-image
gu install native-image

# Build native executable
mvn package -Dnative -DskipTests

# Run native executable
./target/hospital-scheduling-runner
```

## 🔍 Monitoring & Debugging

### Logging

Configure logging levels in `application.properties`:

```properties
# Solver debugging
quarkus.log.category."ai.timefold.solver".level=DEBUG

# Application debugging
quarkus.log.category."org.acme.employeescheduling".level=DEBUG
```

### Health Checks

- **Application health**: `GET /q/health`
- **Solver status**: `GET /hospital-schedule/status`
- **Data validation**: `GET /hospital-schedule/test-data`

## 🤝 Contributing

### Development Setup

1. **Fork the repository**
2. **Create feature branch**: `git checkout -b feature/new-constraint`
3. **Make changes** following the existing code patterns
4. **Add tests** for new functionality
5. **Submit pull request**

### Code Style

- Follow **Java 17** conventions
- Use **meaningful variable names**
- Add **comprehensive comments** for complex logic
- Include **unit tests** for all new features

## 📚 Additional Resources

- [Timefold Solver Documentation](https://docs.timefold.ai)
- [Quarkus Framework Guide](https://quarkus.io/guides)
- [Constraint Programming Concepts](https://docs.timefold.ai/timefold-solver/latest/optimization-algorithms/overview)
- [Hospital Scheduling Best Practices](https://docs.timefold.ai/timefold-solver/latest/use-cases-and-examples/use-cases-and-examples)

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

## 🆘 Support

For issues and questions:
- **GitHub Issues**: [Create an issue](https://github.com/your-repo/issues)
- **Documentation**: [Timefold Docs](https://docs.timefold.ai)
- **Community**: [Timefold Community](https://timefold.ai/community)

---

**Built with ❤️ using Timefold Solver and Quarkus**
