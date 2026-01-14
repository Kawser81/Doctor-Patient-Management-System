# Doctor-Patient Management System

## Overview

A comprehensive healthcare management platform built with Spring Boot that facilitates appointment scheduling, prescription management, and doctor-patient interactions. The system implements role-based access control (RBAC) with three user types: Admin, Doctor, and Patient.

## Key Features

### For Patients
- Complete profile registration with personal details
- Browse and filter doctors by speciality
- View doctor profiles with ratings and reviews
- Real-time availability calendar
- Book appointments with time slot selection
- View appointment history
- Access and download prescriptions as PDF
- Submit reviews and ratings for doctors
- Cancel appointments

### For Doctors
- Professional profile management
- Set consultation hours and fees
- Manage availability calendar
- Block/unblock specific days
- View and manage appointments
- Create and manage prescriptions
- View patient information
- Track reviews and ratings

### For Administrators
- Comprehensive dashboard with statistics
- User management (create, view, delete)
- Appointment oversight and management
- System-wide analytics
- Monitor incomplete profiles
- Cancel appointments
- Generate alerts and reports

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Security**: Spring Security with JWT authentication
- **ORM**: Spring Data JPA with Hibernate
- **Database**: PostgreSQL (Production), H2 (Development)
- **Caching**: Redis
- **Message Queue**: RabbitMQ
- **Validation**: Jakarta Validation
- **PDF Generation**: OpenHTMLtoPDF
- **Template Engine**: Thymeleaf

### Architecture Patterns
- **MVC Pattern**: Model-View-Controller architecture
- **Repository Pattern**: Data access abstraction
- **Service Layer Pattern**: Business logic encapsulation
- **DTO Pattern**: Data transfer objects for API responses
- **Transactional Outbox Pattern**: Reliable message delivery

## Project Structure

```
src/main/java/com/example/doctor_patient_management_system/
├── config/              # Configuration classes
│   ├── JacksonConfig.java
│   ├── RabbitConfig.java
│   ├── RedisConfig.java
│   ├── SecurityConfig.java
│   └── StartupConfig.java
├── controller/          # REST controllers and web controllers
│   ├── AdminController.java
│   ├── AuthController.java
│   ├── DoctorController.java
│   ├── HomeController.java
│   └── PatientController.java
├── dto/                 # Data Transfer Objects
├── model/               # Domain entities
│   ├── User.java
│   ├── Doctor.java
│   ├── Patient.java
│   ├── Appointment.java
│   ├── Prescription.java
│   ├── Review.java
│   └── enumeration/
├── repository/          # Data access layer
├── security/            # Security components
│   ├── JwtUtil.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserPrincipal.java
│   └── SecurityConfig.java
└── service/             # Business logic layer
    ├── impl/            # Service implementations
```

## Core Modules

### 1. Authentication & Authorization
- JWT-based stateless authentication
- Role-based access control (ADMIN, DOCTOR, PATIENT)
- Cookie-based token storage
- Custom UserDetailsService implementation

### 2. Appointment Management
- Real-time slot availability checking
- Automatic past appointment cancellation
- Conflict prevention
- Status tracking (CONFIRMED, CANCELLED)

### 3. Messaging System
- RabbitMQ integration for async messaging
- Transactional outbox pattern
- Booking confirmations
- Registration notifications
- Retry mechanism for failed messages

### 4. Caching Strategy
- Redis-based distributed caching
- Doctor profile caching
- Patient data caching
- Cache eviction on updates

### 5. Calendar & Availability
- Dynamic time slot generation
- Day blocking/unblocking
- Off-day management
- Override mechanism for specific dates

## Documentation

📁 **[Architecture Documentation](./doc/architecture/)**
- [Database Schema & ERD](./doc/architecture/database.md)
- [Class Diagrams](./doc/architecture/classes.md)

📁 **[API Documentation](./doc/api/)**
- [REST API Endpoints](./doc/api/endpoints.md)
- [Web Pages & Navigation](./doc/api/navigation.md)

## Quick Start

### Prerequisites
- Java 17 or higher
- PostgreSQL 14+
- Redis 6+
- RabbitMQ 3.9+
- Maven 3.8+

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd doctor-patient-management-system
```

2. **Configure Database**
```properties
# application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/doctor_patient_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. **Configure Redis**
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

4. **Configure RabbitMQ**
```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

5. **Set JWT Secret**
```properties
jwt.secret=your_base64_encoded_secret_key
```

6. **Build and Run**
```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## Default Access

### Initial Admin Account
Create an admin account through registration and manually set role to ADMIN in the database.

### User Registration Flow
1. Register at `/auth/register`
2. Complete profile at role-specific page
3. Access dashboard based on role

## Key Endpoints

- **Home**: `/`
- **Login**: `/auth/login`
- **Register**: `/auth/register`
- **Admin Dashboard**: `/admin/dashboard`
- **Doctor Profile**: `/doctors/profile`
- **Patient Profile**: `/patients/profile`
- **Available Doctors**: `/available`
- **Doctor List**: `/doctors`

## Security Features

- Password encryption with BCrypt
- JWT token expiration (24 hours)
- HttpOnly cookies
- CSRF protection disabled (stateless API)
- Role-based method security
- SQL injection prevention via JPA

## Performance Optimizations

- Redis caching for frequently accessed data
- Lazy loading for entity relationships
- Connection pooling
- Indexed database columns
- Pagination for large datasets
- Optimistic locking for concurrent updates

## Message Queue Architecture

### Queues
- `booking.queue` - Appointment bookings
- `registration.queue` - User registrations

### Exchange
- `doctor.patient.exchange` (Topic Exchange)

### Routing Keys
- `booking.key`
- `registration.key`

## Scheduled Tasks

- **Outbox Message Processor**: Every 10 seconds
- **Failed Message Retry**: Every 30 seconds
- **Past Appointment Cleanup**: On application startup

## Error Handling

- Global exception handling
- Validation error responses
- Custom error messages
- Transaction rollback on failures

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DoctorServiceTest

# Run with coverage
mvn clean verify
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue in the repository
- Contact: support@example.com

## Version History

- **v1.0.0** - Initial release
  - User authentication and authorization
  - Appointment booking system
  - Prescription management
  - Review and rating system
  - Admin dashboard

## Future Enhancements

- [ ] Email notifications
- [ ] SMS reminders
- [ ] Payment gateway integration
- [ ] Video consultation
- [ ] Mobile application
- [ ] Multi-language support
- [ ] Advanced analytics dashboard
- [ ] Patient health records
- [ ] Telemedicine features
