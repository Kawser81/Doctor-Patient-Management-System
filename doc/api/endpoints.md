# API Endpoints Documentation

## Authentication Endpoints

### POST /auth/register
**Description**: Register a new user account  
**Access**: Public  
**Content-Type**: `application/x-www-form-urlencoded` or `multipart/form-data`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "securePassword123",
  "role": "PATIENT" // or "DOCTOR", "ADMIN"
}
```

**Response**:
- **Success (302)**: Redirect to profile completion page
  - Doctor: `/doctors/complete-registration`
  - Patient: `/patients/complete-registration`
  - Admin: `/admin/dashboard`
- **Error (200)**: Returns registration form with error message

**Sets Cookie**: `JWT_TOKEN` (HttpOnly, 24h expiration)

---

### POST /auth/login
**Description**: Authenticate user and obtain JWT token  
**Access**: Public  
**Content-Type**: `application/x-www-form-urlencoded`

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
- **Success (302)**: Redirect based on role and profile completion
  - Complete Doctor: `/doctors/profile`
  - Complete Patient: `/patients/profile`
  - Admin: `/admin/dashboard`
  - Incomplete: Profile completion page
- **Error (200)**: Returns login form with error message

**Sets Cookie**: `JWT_TOKEN`

---

### GET /auth/logout
**Description**: Logout user and invalidate session  
**Access**: Authenticated  

**Response**:
- **302**: Redirect to `/auth/login?logout=true`
- Clears `JWT_TOKEN` cookie
- Invalidates Spring Security context

---

## Doctor Endpoints

### GET /doctors
**Description**: List all doctors with filtering and pagination  
**Access**: Public  
**Query Parameters**:
- `speciality` (optional): Filter by medical speciality
- `doctorName` (optional): Search by doctor name
- `page` (default: 1): Page number
- `size` (default: 12): Items per page

**Response**: HTML page with doctor list

---

### GET /doctors/{id}/profile
**Description**: View doctor profile with calendar and reviews  
**Access**: Public  
**Path Parameters**:
- `id`: Doctor ID

**Query Parameters**:
- `year` (optional): Calendar year
- `month` (optional): Calendar month (1-12)

**Response**: HTML page with:
- Doctor details
- Monthly availability calendar
- Reviews and ratings
- Booking interface (for patients)

---

### GET /doctors/{id}/api/slots
**Description**: Get available time slots for a specific date  
**Access**: Public  
**Path Parameters**:
- `id`: Doctor ID

**Query Parameters**:
- `date` (required): Appointment date (ISO format: YYYY-MM-DD)

**Response (JSON)**:
```json
[
  {
    "id": 1,
    "slotName": "Morning Slot 1",
    "startTime": "09:00 AM",
    "endTime": "09:20 AM",
    "session": "Morning"
  },
  {
    "id": 2,
    "slotName": "Morning Slot 2",
    "startTime": "09:20 AM",
    "endTime": "09:40 AM",
    "session": "Morning"
  }
]
```

---

### POST /doctors/{id}/api/book
**Description**: Book an appointment (API endpoint)  
**Access**: Authenticated (PATIENT role)  
**Path Parameters**:
- `id`: Doctor ID

**Request Body (JSON)**:
```json
{
  "slotId": 5,
  "appointmentDate": "2026-02-15"
}
```

**Response (JSON)**:
```json
{
  "success": true,
  "message": "Booking confirmed successfully",
  "appointmentId": 123
}
```

**Error Response**:
```json
{
  "success": false,
  "error": "Slot already booked"
}
```

---

### GET /doctors/profile
**Description**: Redirect to authenticated doctor's profile  
**Access**: Authenticated (DOCTOR role)  

**Response**: 302 Redirect to `/doctors/{id}/profile`

---

### GET /doctors/complete-registration
**Description**: Show doctor profile completion form  
**Access**: Authenticated (DOCTOR role, incomplete profile)  

**Response**: HTML form for completing doctor profile

---

### POST /doctors/complete-registration
**Description**: Complete doctor profile registration  
**Access**: Authenticated (DOCTOR role, incomplete profile)  

**Request Body (Form)**:
```
doctorName: "Dr. John Smith"
degree: "MBBS, MD"
speciality: "Cardiology"
consultationStartTime: "09:00"
consultationEndTime: "17:00"
consultationFee: 1000
address: "123 Medical St, Dhaka"
contact: "01712345678"
offDays: "FRIDAY,SATURDAY"
```

**Response**: Redirect to `/doctors/profile`

---

### GET /doctors/{id}/edit
**Description**: Show doctor profile edit form  
**Access**: Authenticated (DOCTOR - own profile, or ADMIN)  

**Response**: HTML edit form

---

### POST /doctors/{id}/edit
**Description**: Update doctor profile  
**Access**: Authenticated (DOCTOR - own profile, or ADMIN)  

**Request Body**: Same as complete-registration  
**Response**: Redirect to `/doctors/{id}/profile?success`

---

### GET /doctors/my-appointments
**Description**: View doctor's appointments  
**Access**: Authenticated (DOCTOR role)  

**Response**: HTML page with appointment list

---

### GET /doctors/appointments/{appointmentId}/prescribe
**Description**: Show prescription form  
**Access**: Authenticated (DOCTOR role, owns appointment)  

**Response**: HTML prescription form

---

### POST /doctors/appointments/{appointmentId}/prescribe
**Description**: Create/update prescription  
**Access**: Authenticated (DOCTOR role, owns appointment)  

**Request Body (Form)**:
```
chiefComplaint: "Patient complaint..."
history: "Medical history..."
examinationFindings: "Examination details..."
diagnosis: "Diagnosis..."
medicines: "Prescribed medications..."
advice: "Medical advice..."
nextVisit: "Follow-up instructions..."
```

**Response**: Redirect to `/doctors/my-appointments?prescription_saved`

---

### POST /doctors/appointments/{appointmentId}/cancel
**Description**: Cancel an appointment  
**Access**: Authenticated (DOCTOR role, owns appointment)  

**Response (JSON)**:
```json
{
  "success": true
}
```

---

### GET /doctors/{id}/block-days
**Description**: Manage blocked days  
**Access**: Authenticated (DOCTOR role, own profile)  

**Response**: HTML page with blocked days list

---

### POST /doctors/{id}/block-day
**Description**: Block a specific date  
**Access**: Authenticated (DOCTOR role, own profile)  

**Request Parameters**:
- `date`: Date to block (YYYY-MM-DD)

**Response**: Redirect to `/doctors/{id}/profile`  
**Note**: Cancels existing confirmed appointments on that date

---

### POST /doctors/{id}/unblock-day
**Description**: Unblock a previously blocked date  
**Access**: Authenticated (DOCTOR role, own profile)  

**Request Parameters**:
- `date`: Date to unblock

**Response**: Redirect to `/doctors/{id}/block-days`

---

### GET /doctors/{id}/reviews
**Description**: View all reviews for a doctor (paginated)  
**Access**: Public  

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 10)

**Response**: HTML page with reviews

---

## Patient Endpoints

### GET /patients/complete-registration
**Description**: Show patient profile completion form  
**Access**: Authenticated (PATIENT role, incomplete profile)  

**Response**: HTML registration form

---

### POST /patients/complete-registration
**Description**: Complete patient profile  
**Access**: Authenticated (PATIENT role, incomplete profile)  

**Request Body (Form)**:
```
patientName: "Jane Doe"
patientEmail: "jane@example.com"
gender: "Female"
contact: "01798765432"
```

**Response**: Redirect to `/patients/profile`

---

### GET /patients/profile
**Description**: View patient dashboard with available doctors  
**Access**: Authenticated (PATIENT role)  

**Query Parameters**:
- `speciality` (optional): Filter doctors by speciality
- `page` (default: 1)
- `size` (default: 10)

**Response**: HTML dashboard with:
- Patient information
- Available doctors list
- Speciality filter

---

### GET /patients/appointments
**Description**: View patient's appointments  
**Access**: Authenticated (PATIENT role)  

**Response**: HTML page with appointment history

---

### POST /patients/appointments/{appointmentId}/cancel
**Description**: Cancel an appointment  
**Access**: Authenticated (PATIENT role, owns appointment)  

**Response (JSON)**:
```json
{
  "success": true
}
```

---

### GET /patients/prescriptions/{appointmentId}
**Description**: View prescription for an appointment  
**Access**: Authenticated (PATIENT role, owns appointment)  

**Response**: HTML prescription view

---

### GET /patients/prescriptions/{appointmentId}/download-pdf
**Description**: Download prescription as PDF  
**Access**: Authenticated (PATIENT role, owns appointment)  

**Response**: PDF file download
**Content-Type**: `application/pdf`

---

### POST /patients/reviews
**Description**: Submit a review for an appointment  
**Access**: Authenticated (PATIENT role)  

**Request Parameters**:
- `appointmentId`: Appointment to review
- `rating`: Rating (1-5)
- `comment` (optional): Review text

**Response (JSON)**:
```json
{
  "success": true,
  "reviewId": 45,
  "error": null
}
```

---

## Admin Endpoints

### GET /admin/dashboard
**Description**: Admin dashboard with statistics and management tools  
**Access**: Authenticated (ADMIN role)  

**Query Parameters (for users table)**:
- `userPage` (default: 0)
- `userSize` (default: 10)
- `role` (optional): Filter by user role
- `userSearch` (optional): Search by email

**Query Parameters (for appointments table)**:
- `appointmentPage` (default: 0)
- `appointmentSize` (default: 10)
- `statusFilter` (optional): Filter by status
- `appointmentSearch` (optional): Search by patient/doctor
- `startDate` (optional): Filter from date
- `endDate` (optional): Filter to date

**Response**: HTML dashboard with:
- System statistics
- User management table
- Appointment management table
- Recent registrations
- Alerts

---

### POST /admin/users/{id}/delete
**Description**: Delete a user and related data  
**Access**: Authenticated (ADMIN role)  
**Path Parameters**:
- `id`: User ID to delete

**Response**: Redirect to `/admin/dashboard`  
**Note**: Cascades to delete doctor/patient profiles and appointments

---

### POST /admin/appointments/{id}/cancel
**Description**: Cancel any appointment  
**Access**: Authenticated (ADMIN role)  

**Response**: Redirect to `/admin/dashboard`

---

### GET /admin/appointments/{id}/view
**Description**: View appointment details  
**Access**: Authenticated (ADMIN role)  

**Response**: HTML appointment detail view

---

## Public Endpoints

### GET /
**Description**: Application home page  
**Access**: Public  

**Response**: HTML landing page

---

### GET /available
**Description**: View available doctors  
**Access**: Public  

**Query Parameters**:
- `page` (default: 1)
- `size` (default: 12)

**Response**: HTML page with available doctors (next 7 days)

---

## Error Responses

### Common HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success (for HTML responses) |
| 302 | Redirect |
| 400 | Bad Request (validation errors) |
| 401 | Unauthorized (not logged in) |
| 403 | Forbidden (insufficient permissions) |
| 404 | Resource Not Found |
| 500 | Internal Server Error |

### Error Response Format (JSON endpoints)

```json
{
  "success": false,
  "error": "Error message description",
  "message": null,
  "appointmentId": null
}
```

## Authentication

### JWT Token
- **Storage**: HttpOnly cookie named `JWT_TOKEN`
- **Expiration**: 24 hours
- **Format**: `Bearer <token>`
- **Claims**:
  - `sub`: User email
  - `role`: User role (ADMIN/DOCTOR/PATIENT)
  - `iat`: Issued at timestamp
  - `exp`: Expiration timestamp

### Authorization Header (alternative)
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Rate Limiting

Currently not implemented. Recommended for production:
- 100 requests per minute per IP
- 1000 requests per hour per user

## CORS

CORS is not configured. For frontend applications:
```java
@CrossOrigin(origins = "http://localhost:3000")
```

## API Versioning

Current version: **v1** (implicit)  
No versioning in URLs currently. For future:
- URL versioning: `/api/v1/doctors`
- Header versioning: `Accept: application/vnd.api.v1+json`

---

**Base URL**: `http://localhost:8080`  
**Content Types**: `application/json`, `application/x-www-form-urlencoded`, `multipart/form-data`  
**Authentication**: JWT in Cookie or Authorization header