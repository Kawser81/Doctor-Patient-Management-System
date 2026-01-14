# UML Class Diagrams

## Domain Model - Core Entities

```mermaid
classDiagram
    class User {
        -Long id
        -String email
        -String password
        -Role role
        -boolean complete
        -Doctor doctor
        -List~Appointment~ appointmentsAsPatient
        +getId() Long
        +getEmail() String
        +getRole() Role
        +isComplete() boolean
    }

    class Doctor {
        -Long id
        -String doctorName
        -String speciality
        -String email
        -String degree
        -String consultationStartTime
        -String consultationEndTime
        -String address
        -String contact
        -Integer consultationFee
        -String offDays
        -Long version
        -User user
        -List~Appointment~ appointments
        +getId() Long
        +getDoctorName() String
        +getSpeciality() String
    }

    class Patient {
        -Long id
        -String patientName
        -String patientEmail
        -String gender
        -String contact
        -User user
        +getId() Long
        +getPatientName() String
    }

    class Appointment {
        -Long id
        -Doctor doctor
        -User patient
        -Integer slotId
        -LocalDate appointmentDate
        -String appointmentTime
        -AppointmentStatus status
        -Prescription prescription
        +getId() Long
        +getDoctor() Doctor
        +getStatus() AppointmentStatus
    }

    class Prescription {
        -Long id
        -Appointment appointment
        -String chiefComplaint
        -String history
        -String examinationFindings
        -String diagnosis
        -String medicines
        -String advice
        -String nextVisit
        -LocalDateTime createdAt
        +getId() Long
        +getAppointment() Appointment
    }

    class Review {
        -Long id
        -Appointment appointment
        -Integer rating
        -String comment
        -LocalDateTime createdAt
        +getId() Long
        +getRating() Integer
    }

    class DoctorAvailability {
        -Long id
        -Doctor doctor
        -DayOfWeek dayOfWeek
        -LocalTime startTime
        -LocalTime endTime
        -Boolean isAvailable
        +getDayOfWeek() DayOfWeek
    }

    class DoctorAvailabilityOverride {
        -Long id
        -Doctor doctor
        -LocalDate overrideDate
        -Boolean isAvailable
        +getOverrideDate() LocalDate
    }

    class MessageOutbox {
        -Long id
        -String messageType
        -String payload
        -String routingKey
        -LocalDateTime createdAt
        -LocalDateTime sentAt
        -String status
        -Integer retryCount
        -String errorMessage
        +getStatus() String
    }

    class Role {
        <<enumeration>>
        ADMIN
        DOCTOR
        PATIENT
    }

    class AppointmentStatus {
        <<enumeration>>
        CONFIRMED
        CANCELLED
    }

    User "1" -- "0..1" Doctor : has profile
    User "1" -- "0..1" Patient : has profile
    User "1" -- "*" Appointment : books as patient
    Doctor "1" -- "*" Appointment : has
    Doctor "1" -- "*" DoctorAvailability : defines
    Doctor "1" -- "*" DoctorAvailabilityOverride : blocks
    Appointment "1" -- "0..1" Prescription : has
    Appointment "1" -- "0..1" Review : receives
    User ..> Role : uses
    Appointment ..> AppointmentStatus : uses
```

## Service Layer Architecture

```mermaid
classDiagram
    class UserService {
        <<interface>>
        +registerUser(User) User
        +authenticate(String, String) User
        +findByEmail(String) Optional~User~
        +markProfileComplete(Long) void
    }

    class UserServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +registerUser(User) User
        +authenticate(String, String) User
        +markProfileComplete(Long) void
    }

    class DoctorService {
        <<interface>>
        +findAll() List~Doctor~
        +createDoctorProfile(Long, DoctorDto) Doctor
        +updateDoctorProfile(Long, DoctorDto) Doctor
        +getDoctorById(Long) Doctor
        +blockDay(Long, LocalDate) void
    }

    class DoctorServiceImpl {
        -DoctorRepository doctorRepository
        -UserRepository userRepository
        -ReviewRepository reviewRepository
        +createDoctorProfile(Long, DoctorDto) Doctor
        +updateDoctorProfile(Long, DoctorDto) Doctor
        +getAverageRatingForDoctor(Long) Double
    }

    class PatientService {
        <<interface>>
        +createPatientProfile(Long, PatientDto, User) Patient
        +updatePatientProfile(Long, PatientDto) Patient
        +bookAppointment(Long, Long, LocalDate, String) Appointment
        +submitReview(Long, Long, Integer, String) Review
    }

    class PatientServiceImpl {
        -PatientRepository patientRepository
        -AppointmentRepository appointmentRepository
        -ReviewRepository reviewRepository
        -RabbitProducerService rabbitProducerService
        +bookAppointment(Long, Long, LocalDate, String) Appointment
        +submitReview(Long, Long, Integer, String) Review
    }

    class AppointmentService {
        <<interface>>
        +getById(Long) Appointment
        +bookAppointment(Appointment) Appointment
        +cancelAppointment(Appointment) void
    }

    class AppointmentServiceImpl {
        -AppointmentRepository appointmentRepository
        +bookAppointment(Appointment) Appointment
        +cancelConfirmedAppointmentsForDoctorOnDate(Long, LocalDate) int
    }

    class AdminService {
        <<interface>>
        +deleteUser(Long) String
        +getDashboardStats() DashboardStats
        +getUsers(int, int, String, String) Page~User~
        +cancelAppointment(Long) void
    }

    class AdminServiceImpl {
        -DoctorRepository doctorRepository
        -PatientRepository patientRepository
        -AppointmentRepository appointmentRepository
        -UserRepository userRepository
        +deleteUser(Long) String
        +getDashboardStats() DashboardStats
    }

    UserService <|.. UserServiceImpl : implements
    DoctorService <|.. DoctorServiceImpl : implements
    PatientService <|.. PatientServiceImpl : implements
    AppointmentService <|.. AppointmentServiceImpl : implements
    AdminService <|.. AdminServiceImpl : implements
```

## Repository Layer

```mermaid
classDiagram
    class JpaRepository~T,ID~ {
        <<interface>>
        +findAll() List~T~
        +findById(ID) Optional~T~
        +save(T) T
        +delete(T) void
    }

    class UserRepository {
        <<interface>>
        +findByEmail(String) Optional~User~
        +findByRole(Role, Pageable) Page~User~
        +countByRole(Role) long
        +countByCompleteFalse() long
    }

    class DoctorRepository {
        <<interface>>
        +findByEmail(String) Optional~Doctor~
        +findByUserEmail(String) Optional~Doctor~
        +findByUserId(Long) Optional~Doctor~
        +findBySpecialityIgnoreCase(String) List~Doctor~
        +findDistinctSpecialityOrderBySpecialityAsc() List~String~
        +findAllWithAverageRatings() List~DoctorWithRatingDto~
    }

    class PatientRepository {
        <<interface>>
        +findByPatientEmail(String) Optional~Patient~
        +findByUserId(Long) Optional~Patient~
    }

    class AppointmentRepository {
        <<interface>>
        +findByDoctorIdAndAppointmentDateBetween(Long, LocalDate, LocalDate) List~Appointment~
        +findBookedSlotIdsByDoctorIdAndDate(Long, LocalDate) List~Integer~
        +findByPatientIdOrderByAppointmentDateDesc(Long) List~Appointment~
        +findByDoctorIdAndAppointmentDateAndStatus(Long, LocalDate, AppointmentStatus) List~Appointment~
    }

    class ReviewRepository {
        <<interface>>
        +findByAppointmentId(Long) Optional~Review~
        +existsByAppointmentId(Long) boolean
        +findAverageRatingByDoctorId(Long) Double
        +countReviewsByDoctorId(Long) Long
        +findReviewsWithPatientByDoctorId(Long, Pageable) Page~ReviewDto~
    }

    class PrescriptionRepository {
        <<interface>>
        +findByAppointmentId(Long) Optional~Prescription~
    }

    JpaRepository <|-- UserRepository : extends
    JpaRepository <|-- DoctorRepository : extends
    JpaRepository <|-- PatientRepository : extends
    JpaRepository <|-- AppointmentRepository : extends
    JpaRepository <|-- ReviewRepository : extends
    JpaRepository <|-- PrescriptionRepository : extends
```

## Controller Layer

```mermaid
classDiagram
    class AuthController {
        -RabbitProducerService rabbitProducerService
        -PasswordEncoder passwordEncoder
        -JwtUtil jwtUtil
        -UserService userService
        +showRegisterForm(Model) String
        +registerUser(User, BindingResult, HttpServletResponse, HttpServletRequest) String
        +showLoginForm(Model) String
        +login(AuthRequest, BindingResult, HttpServletResponse) String
        +logout(HttpServletRequest, HttpServletResponse) String
    }

    class DoctorController {
        -DoctorService doctorService
        -AppointmentService appointmentService
        -PrescriptionService prescriptionService
        +doctorProfile(Long, Model, int, int) String
        +listDoctors(String, String, int, int, Model) String
        +editProfile(Long, Model, Authentication) String
        +updateProfile(Long, DoctorDto, BindingResult, Authentication) String
        +myAppointments(UserPrincipal, Model) String
        +showPrescribeForm(Long, UserPrincipal, Model) String
        +savePrescription(Long, Prescription, BindingResult) String
        +blockDay(Long, LocalDate) String
        +unblockDay(Long, LocalDate) String
    }

    class PatientController {
        -PatientService patientService
        -AppointmentService appointmentService
        -DoctorService doctorService
        +showRegisterForm(UserPrincipal, Model) String
        +completePatientRegistration(UserPrincipal, PatientDto, BindingResult) String
        +viewProfile(UserPrincipal, String, int, int, Model) String
        +myAppointments(UserPrincipal, Model) String
        +viewPrescription(Long, UserPrincipal, Model) String
        +downloadPrescriptionPdf(Long, UserPrincipal) ResponseEntity
        +cancelAppointment(Long, UserPrincipal) ResponseEntity
        +submitReview(Long, Integer, String, UserPrincipal) ResponseEntity
    }

    class AdminController {
        -AdminService adminService
        +dashboard(int, int, String, String, Model) String
        +deleteUser(Long, UserPrincipal) String
        +cancelAppointment(Long) String
        +viewAppointment(Long, Model) String
    }

    class HomeController {
        -DoctorAvailabilityService doctorAvailabilityService
        +home() String
        +getAvailableDoctors(int, int, Model) String
    }

    AuthController ..> UserService : uses
    DoctorController ..> DoctorService : uses
    DoctorController ..> AppointmentService : uses
    PatientController ..> PatientService : uses
    AdminController ..> AdminService : uses
```

## Security Layer

```mermaid
classDiagram
    class SecurityConfig {
        -JwtAuthenticationFilter jwtFilter
        -UserDetailsService userDetailsService
        +filterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager(AuthenticationConfiguration) AuthenticationManager
    }

    class JwtUtil {
        -Key key
        -long jwtExpirationMs
        +generateToken(String, String) String
        +validateToken(String) boolean
        +getUsernameFromToken(String) String
        +getRoleFromToken(String) String
    }

    class JwtAuthenticationFilter {
        -JwtUtil jwtUtil
        -CustomUserDetailsService userDetailsService
        +doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain) void
    }

    class UserPrincipal {
        -User user
        +getAuthorities() Collection~GrantedAuthority~
        +getPassword() String
        +getUsername() String
        +getUser() User
    }

    class CustomUserDetailsService {
        -UserRepository userRepository
        +loadUserByUsername(String) UserDetails
    }

    SecurityConfig ..> JwtAuthenticationFilter : uses
    JwtAuthenticationFilter ..> JwtUtil : uses
    JwtAuthenticationFilter ..> CustomUserDetailsService : uses
    CustomUserDetailsService ..> UserPrincipal : creates
    UserPrincipal ..> User : wraps
```

## Messaging Layer

```mermaid
classDiagram
    class RabbitProducerService {
        <<interface>>
        +saveBookingMessage(BookingMessage) void
        +saveRegistrationMessage(RegistrationMessage) void
        +sendToQueue(MessageOutbox) boolean
    }

    class RabbitProducerServiceImpl {
        -RabbitTemplate rabbitTemplate
        -MessageOutboxRepository outboxRepository
        -ObjectMapper objectMapper
        +saveBookingMessage(BookingMessage) void
        +saveRegistrationMessage(RegistrationMessage) void
        +sendToQueue(MessageOutbox) boolean
    }

    class RabbitConsumerService {
        <<interface>>
        +consumeBookingMessage(BookingMessage) void
        +consumeRegistrationMessage(RegistrationMessage) void
    }

    class RabbitConsumerServiceImpl {
        +consumeBookingMessage(BookingMessage) void
        +consumeRegistrationMessage(RegistrationMessage) void
    }

    class MessageOutboxScheduler {
        <<interface>>
        +processPendingMessages() void
        +retryFailedMessages() void
    }

    class MessageOutboxSchedulerImpl {
        -MessageOutboxRepository outboxRepository
        -RabbitProducerService producerService
        +processPendingMessages() void
        +retryFailedMessages() void
    }

    class BookingMessage {
        -Long appointmentId
        -String patientEmail
        -String patientName
        -String doctorEmail
        -String doctorName
        -LocalDate appointmentDate
        -String appointmentTime
        -String speciality
        -Integer consultationFee
    }

    class RegistrationMessage {
        -String userEmail
        -String role
        -String fullName
    }

    RabbitProducerService <|.. RabbitProducerServiceImpl : implements
    RabbitConsumerService <|.. RabbitConsumerServiceImpl : implements
    MessageOutboxScheduler <|.. MessageOutboxSchedulerImpl : implements
    RabbitProducerServiceImpl ..> BookingMessage : uses
    RabbitProducerServiceImpl ..> RegistrationMessage : uses
    RabbitConsumerServiceImpl ..> BookingMessage : consumes
    RabbitConsumerServiceImpl ..> RegistrationMessage : consumes
```

## DTO Layer

```mermaid
classDiagram
    class DoctorDto {
        -String doctorName
        -String email
        -String degree
        -String speciality
        -String consultationStartTime
        -String consultationEndTime
        -String offDays
        -Integer consultationFee
        -String address
        -String contact
    }

    class PatientDto {
        -String patientName
        -String patientEmail
        -String gender
        -String contact
    }

    class AuthRequest {
        -String email
        -String password
        -String role
    }

    class AuthResponse {
        -String token
        -String tokenType
    }

    class TimeSlot {
        -int id
        -String slotName
        -String startTime
        -String endTime
        -String session
    }

    class DayStatus {
        -LocalDate date
        -String status
        -int bookedCount
        -int availableCount
        -boolean isToday
        -String dayName
    }

    class ReviewDto {
        -Integer rating
        -String comment
        -LocalDateTime createdAt
        -String patientName
    }

    class DoctorWithRatingDto {
        -Doctor doctor
        -Double averageRating
        -Long reviewCount
    }

    class AvailableDoctorSummary {
        -Doctor doctor
        -LocalDate nextAvailableDate
        -int availableSlotsNextWeek
    }

    class BookAppointmentRequest {
        -int slotId
        -LocalDate appointmentDate
    }

    class BookAppointmentResponse {
        -boolean success
        -String message
        -Long appointmentId
        -String error
    }

    class DashboardStats {
        -long totalAdmins
        -long totalDoctors
        -long totalPatients
        -long totalUsers
        -long totalAppointments
        -long confirmedAppointments
        -long cancelledAppointments
    }

    class DoctorAppointmentDto {
        -Appointment appointment
        -Patient patientProfile
        -Prescription prescription
    }
```

## Configuration Classes

```mermaid
classDiagram
    class JacksonConfig {
        +objectMapper() ObjectMapper
        +jackson2ObjectMapperBuilder() Jackson2ObjectMapperBuilder
    }

    class RabbitConfig {
        +BOOKING_QUEUE$ String
        +REGISTRATION_QUEUE$ String
        +EXCHANGE$ String
        +BOOKING_ROUTING_KEY$ String
        +REGISTRATION_ROUTING_KEY$ String
        +bookingQueue() Queue
        +registrationQueue() Queue
        +exchange() TopicExchange
        +bookingBinding(Queue, TopicExchange) Binding
        +registrationBinding(Queue, TopicExchange) Binding
        +jsonMessageConverter() MessageConverter
        +rabbitTemplate(ConnectionFactory) RabbitTemplate
    }

    class RedisConfig {
        +redisObjectMapper() ObjectMapper
        +redisTemplate(RedisConnectionFactory) RedisTemplate
        +cacheManager(RedisConnectionFactory) CacheManager
    }

    class SecurityConfig {
        +filterChain(HttpSecurity) SecurityFilterChain
        +passwordEncoder() PasswordEncoder
        +authenticationManager(AuthenticationConfiguration) AuthenticationManager
    }

    class StartupConfig {
        +autoCancelPastAppointmentsOnStartup(AppointmentRepository) CommandLineRunner
    }
```

## Key Design Patterns Used

### 1. **Repository Pattern**
- Abstracts data access logic
- Provides clean interface for CRUD operations
- Implemented via Spring Data JPA

### 2. **Service Layer Pattern**
- Encapsulates business logic
- Transactional boundaries
- Clear separation of concerns

### 3. **DTO Pattern**
- Data transfer between layers
- Prevents entity exposure
- Validation at boundaries

### 4. **Transactional Outbox Pattern**
- Reliable messaging
- Atomic database + message queue operations
- Retry mechanism for failures

### 5. **Strategy Pattern**
- Different user role behaviors
- Service implementations for each role
- Role-based access control

### 6. **Factory Pattern**
- TimeSlot generation
- Calendar day status creation

### 7. **Template Method Pattern**
- Spring's Template classes (JdbcTemplate, RabbitTemplate)
- Consistent error handling

---

**Architecture**: Layered Architecture (Presentation → Service → Repository → Database)  
**Framework**: Spring Boot with dependency injection  
**Persistence**: JPA/Hibernate ORM