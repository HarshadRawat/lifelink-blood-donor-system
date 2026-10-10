# LifeLink — REST API Specification & Endpoint Reference

Interactive OpenAPI 3 / Swagger UI is available at:
`http://localhost:8080/swagger-ui.html`

All protected endpoints require an HTTP `Authorization` header formatted as:
`Authorization: Bearer <your_jwt_token>`

---

## 1. Authentication Endpoints

### Register User
* **Method**: `POST`
* **Path**: `/api/auth/register`
* **Access**: Public
* **Request Body**:
```json
{
  "name": "Apollo Emergency Hospital",
  "email": "apollo@hospital.org",
  "password": "Password123!",
  "role": "HOSPITAL"
}
```
*Note: Permitted roles are `DONOR` and `HOSPITAL`. `ADMIN` registration via public endpoint returns `403 Forbidden`.*
* **Response `200 OK`**:
```json
{
  "id": 1,
  "name": "Apollo Emergency Hospital",
  "email": "apollo@hospital.org",
  "role": "HOSPITAL"
}
```
* **Error Responses**: `400 Bad Request` (Validation error), `409 Conflict` (Email already exists).

---

### Login User
* **Method**: `POST`
* **Path**: `/api/auth/login`
* **Access**: Public
* **Request Body**:
```json
{
  "email": "apollo@hospital.org",
  "password": "Password123!"
}
```
* **Response `200 OK`**:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "user": {
    "id": 1,
    "name": "Apollo Emergency Hospital",
    "email": "apollo@hospital.org",
    "role": "HOSPITAL"
  }
}
```
* **Error Responses**: `401 Unauthorized` (Invalid credentials).

---

## 2. Donor Profile & Matching Endpoints

### Get Donor Profile
* **Method**: `GET`
* **Path**: `/api/donors/profile`
* **Access**: `ROLE_DONOR`
* **Response `200 OK`**:
```json
{
  "id": 5,
  "bloodGroup": "O+",
  "address": "Indiranagar 100ft Road",
  "city": "Bangalore",
  "latitude": 12.9750,
  "longitude": 77.5990,
  "contactNumber": "9876543210",
  "available": true
}
```

### Create / Update Donor Profile
* **Method**: `POST` / `PUT`
* **Path**: `/api/donors/profile`
* **Access**: `ROLE_DONOR`
* **Request Body**:
```json
{
  "bloodGroup": "O+",
  "address": "Indiranagar 100ft Road",
  "city": "Bangalore",
  "latitude": 12.9750,
  "longitude": 77.5990,
  "contactNumber": "9876543210",
  "available": true
}
```

### Toggle Donor Availability
* **Method**: `PATCH`
* **Path**: `/api/donors/profile/availability`
* **Access**: `ROLE_DONOR`
* **Request Body**: `{"available": true}`

### Get Matches for Authenticated Donor
* **Method**: `GET`
* **Path**: `/api/donors/matches`
* **Access**: `ROLE_DONOR`
* **Response `200 OK`**: Array of `RequestDonorMatchResponse` items.

---

## 3. Hospital Profile Endpoints

### Get / Create / Update Hospital Profile
* **Method**: `GET` / `POST` / `PUT`
* **Path**: `/api/hospitals/profile`
* **Access**: `ROLE_HOSPITAL`
* **Request Body**:
```json
{
  "hospitalName": "Apollo Emergency Care",
  "registrationNumber": "HOSP-REG-4491",
  "address": "120 Health Boulevard",
  "city": "Bangalore",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "contactNumber": "080-22223333"
}
```

---

## 4. Emergency Request & Matching Endpoints

### Create Emergency Blood Request
* **Method**: `POST`
* **Path**: `/api/emergency-requests`
* **Access**: `ROLE_HOSPITAL`
* **Request Body**:
```json
{
  "bloodGroup": "O+",
  "unitsRequired": 2,
  "urgency": "CRITICAL"
}
```
* **Response `200 OK`**:
```json
{
  "id": 10,
  "hospitalId": 1,
  "hospitalName": "Apollo Emergency Care",
  "bloodGroup": "O+",
  "unitsRequired": 2,
  "urgency": "CRITICAL",
  "status": "ACTIVE",
  "createdAt": "2026-10-10T14:30:00"
}
```

### Get Hospital Emergency Requests (Paginated & Filtered)
* **Method**: `GET`
* **Path**: `/api/emergency-requests?page=0&size=6&sort=createdAt,desc&status=ACTIVE&bloodGroup=O+`
* **Access**: `ROLE_HOSPITAL`
* **Response `200 OK`**: Spring `Page<EmergencyRequestResponse>` metadata envelope.

### Trigger Algorithmic Matching
* **Method**: `POST`
* **Path**: `/api/emergency-requests/{requestId}/match`
* **Access**: `ROLE_HOSPITAL`
* **Response `200 OK`**: List of created/identified matches within 20 km radius sorted by nearest distance.

### View Request Matches (Hospital View)
* **Method**: `GET`
* **Path**: `/api/emergency-requests/{requestId}/matches`
* **Access**: `ROLE_HOSPITAL` (Owning hospital only)
* **Privacy Rule**: Donor `contactNumber` and `email` are returned as `null` until the donor explicitly accepts with consent.
* **Response `200 OK`**:
```json
[
  {
    "id": 25,
    "emergencyRequestId": 10,
    "donorId": 5,
    "donorName": "Priya Sharma",
    "bloodGroup": "O+",
    "status": "ACCEPTED",
    "contactNumber": "9876543210",
    "email": "priya@donor.org",
    "contactSharingConsent": true,
    "createdAt": "2026-10-10T14:31:00"
  }
]
```

### Get Authorized Donor Contact Details
* **Method**: `GET`
* **Path**: `/api/emergency-requests/{requestId}/matches/{matchId}/contact`
* **Access**: `ROLE_HOSPITAL` (Owning hospital only)
* **Response `200 OK`**:
```json
{
  "matchId": 25,
  "requestId": 10,
  "donorId": 5,
  "donorName": "Priya Sharma",
  "bloodGroup": "O+",
  "contactNumber": "9876543210",
  "email": "priya@donor.org",
  "status": "ACCEPTED",
  "contactSharingConsent": true
}
```
* **Error Responses**: `403 Forbidden` (Unauthorized hospital), `409 Conflict` (Match not yet accepted or request cancelled).

### Donor Accept / Decline Match
* **Method**: `PATCH`
* **Path**: `/api/emergency-requests/matches/{matchId}/status`
* **Access**: `ROLE_DONOR` (Owning donor only)
* **Request Body**:
```json
{
  "status": "ACCEPTED",
  "consentToShareContact": true
}
```
* **Response `200 OK`**: Updated match DTO.

### Hospital Complete Donation
* **Method**: `PATCH`
* **Path**: `/api/emergency-requests/matches/{matchId}/complete`
* **Access**: `ROLE_HOSPITAL` (Owning hospital only)
* **Response `200 OK`**: Match marked as `COMPLETED`.

---

## 5. Notification Endpoints

### Get Notifications for User
* **Method**: `GET`
* **Path**: `/api/notifications`
* **Access**: Authenticated user

### Mark Notification as Read
* **Method**: `PATCH`
* **Path**: `/api/notifications/{notificationId}/read`
* **Access**: Authenticated user (Recipient only)

### Get Unread Notification Count
* **Method**: `GET`
* **Path**: `/api/notifications/unread-count`
* **Access**: Authenticated user
* **Response `200 OK`**: `{"unreadCount": 3}`
