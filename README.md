# MRP – Media Ratings Platform

**Lightweight RESTful HTTP server** built with **Java 21** using only JDK's built-in `HttpServer`.

**No frameworks** – Pure Java implementation with JDBC for PostgreSQL.

**Modern architecture** – Clean layered design following SOLID principles with revolutionary interface simplification.

---

## Key Features

 **User Authentication** – Register, login with PBKDF2 password hashing
 **Media Management** – Create, read, update, delete movies/series/games (CRUD)
 **Rating System** – Rate media 1-5 stars with optional comments
 **Favorites** – Mark media as favorites, view favorite list
 **Search** – Search media by title with partial matching
 **Authorization** – Token-based auth, ownership verification
 **UUID Identifiers** – All entities use UUID (v7) for better scalability

---

## Architecture Highlights

### Interface Simplification



- **IRepository** – Single interface for all repositories (User, Media, Rating, Favorite)
- **IService** – Single interface for all services (Auth, Media, Rating, Favorite)


### Clean Layers

```
HTTP Layer (Handlers) 
    ↓
Business Logic (Services implementing IService)
    ↓
Data Access (Repositories implementing IRepository)
    ↓
Database (PostgreSQL)
```

### Template Method Pattern

Each repository/service implements the template interface and uses only the methods it needs. Unused methods throw `UnsupportedOperationException`.

---

## Quick Start

### Prerequisites

- **Java 21+**
- **Maven 3.8+**
- **Docker** (for PostgreSQL)

### 1. Start Database

```bash
docker compose up -d
docker compose ps
```

The database will auto-initialize with schema from `db/init/*.sql`.

**Database details:**
- Host: `localhost:5432`
- Database: `mrp_db`
- User: `mrp_user`
- Password: `mrp_password`

### 2. Compile & Run Server

```bash
# Compile
mvn clean compile

# Run
java -cp target/classes at.fhtw.mrp.Main
```

**Expected output:**
```
╔════════════════════════════════════════════════╗
║          MRP Server Started Successfully!                    ║
╚════════════════════════════════════════════════╝

Server running at: http://localhost:8080

Available endpoints:
   /api/users/*        - User authentication (register, login)
   /api/media/*        - Media entries (movies, series, games)
   /api/ratings/*      - Ratings management
   /api/favorites/*    - Favorites management

Architecture:
  🔹 Single IRepository interface for all repositories
  🔹 Single IService interface for all services
  🔹 Clean layered architecture (Handler → Service → Repository)
```

---

## API Endpoints

### Users

#### Register New User
```http
POST /api/users/register
Content-Type: application/json

{
  "username": "alice",
  "password": "s3cret123"
}
```

**Response**: `201 Created`
```json
{
  "message": "User registered successfully"
}
```

#### Login
```http
POST /api/users/login
Content-Type: application/json

{
  "Username": "alice",
  "Password": "s3cret123"
}
```

**Response**: `200 OK`
```json
{
  "token": "b264b73b-5b02-4caa-9279-e827af3de9e8;alice;c13aebac-ef73-4a91-918b-79e70c063be1"
}
```

**Save this token!** Use it in `Authorization: Bearer <token>` header for authenticated requests.

#### Get User Profile
```http
GET /api/users/{username}/profile
Authorization: Bearer <token>
```

**Response**: `200 OK`
```json
{
  "id": "b264b73b-5b02-4caa-9279-e827af3de9e8",
  "username": "alice"
}
```

---

### Media

#### List All Media
```http
GET /api/media
```

**Response**: `200 OK` – Array of all media entries

#### Search Media by Title
```http
GET /api/media?query=matrix
```

**Response**: `200 OK` – Array of matching media (case-insensitive partial match)

#### Get Single Media
```http
GET /api/media/{mediaId}
```

**Response**: `200 OK`
```json
{
  "id": "912420c6-e677-4cd1-9b6f-0f74365201af",
  "ownerId": "b264b73b-5b02-4caa-9279-e827af3de9e8",
  "title": "The Matrix",
  "description": "Sci-fi classic",
  "mediaType": "movie",
  "releaseYear": 1999,
  "genres": "sci-fi,action",
  "ageRestriction": 16,
  "averageScore": 4.5
}
```

#### Create Media
```http
POST /api/media
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "The Matrix",
  "description": "Sci-fi classic",
  "mediaType": "movie",
  "releaseYear": 1999,
  "genres": "sci-fi,action",
  "ageRestriction": 16
}
```

**Response**: `201 Created`
```json
{
  "id": "912420c6-e677-4cd1-9b6f-0f74365201af"
}
```

#### Update Media
```http
PUT /api/media/{mediaId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "The Matrix Reloaded",
  "description": "Updated description",
  "mediaType": "movie",
  "releaseYear": 2003,
  "genres": "sci-fi,action",
  "ageRestriction": 16
}
```

**Response**: `200 OK`
**Note**: Only the owner can update media

#### Delete Media
```http
DELETE /api/media/{mediaId}
Authorization: Bearer <token>
```

**Response**: `204 No Content`
**Note**: Only the owner can delete media

---

### Ratings

#### List Ratings for Media
```http
GET /api/media/{mediaId}/ratings
```

**Response**: `200 OK` – Array of ratings for this media

#### Create Rating
```http
POST /api/media/{mediaId}/ratings
Authorization: Bearer <token>
Content-Type: application/json

{
  "stars": 5,
  "comment": "Amazing movie!"
}
```

**Response**: `201 Created`
```json
{
  "id": "9cd5928f-2ee0-44a0-9a09-57e5239203a4"
}
```

**Note**: Stars must be 1-5. One rating per user per media.

#### Delete Rating
```http
DELETE /api/ratings/{ratingId}
Authorization: Bearer <token>
```

**Response**: `204 No Content`
**Note**: Only the rating creator can delete their rating

---

### Favorites

#### Get User's Favorites
```http
GET /api/favorites
Authorization: Bearer <token>
```

**Response**: `200 OK` – Array of media entries the user has favorited

#### Add to Favorites
```http
POST /api/favorites
Authorization: Bearer <token>
Content-Type: application/json

{
  "mediaId": "912420c6-e677-4cd1-9b6f-0f74365201af"
}
```

**Response**: `201 Created`
```json
{
  "message": "Added to favorites"
}
```

#### Check if Media is Favorited
```http
GET /api/favorites/{mediaId}/status
Authorization: Bearer <token>
```

**Response**: `200 OK`
```json
{
  "isFavorite": true
}
```

#### Remove from Favorites
```http
DELETE /api/favorites/{mediaId}
Authorization: Bearer <token>
```

**Response**: `200 OK`
```json
{
  "message": "Removed from favorites"
}
```

---

## Testing with Postman

### Import Collection & Environment

1. **Collection**: `postman/MRP_DYNAMIC_USER.postman_collection.json`
2. **Environment**: `postman/MRP_Local.postman_environment.json`

### Dynamic User Support

The Postman collection now supports **any user** dynamically:

**Environment Variables** (auto-managed):
```
baseUrl = http://localhost:8080
username = (auto-filled on login)
token = (auto-filled on login)
mediaId = (auto-filled on media creation)
ratingId = (auto-filled on rating creation)
```

### Test Workflow

1. **Register** → Creates user
2. **Login** → Auto-saves `username` and `token`
3. **Create Media** → Auto-saves `mediaId`
4. **Get Media** → Uses saved `mediaId`
5. **Create Rating** → Auto-saves `ratingId`
6. **Add to Favorites** → Uses saved `mediaId`
7. **Get Favorites** → Shows user's favorites

### Switch Users

Simply login with different credentials:
```json
{ "Username": "bob", "Password": "bob123" }
```

The collection automatically updates to use Bob's token and context!

---

## Project Structure

```
src/main/java/at/fhtw/mrp/
├── Main.java                    # Application entry point with DI
│
├── config/
│   └── AppConfig.java           # Environment configuration
│
├── db/
│   └── Database.java            # JDBC connection management
│
├── model/
│   ├── User.java                # User entity
│   ├── MediaEntry.java          # Media entity
│   ├── Rating.java              # Rating entity
│   └── Favorite.java            # Favorite entity
│
├── repo/
│   ├── IRepository.java         # Single template interface
│   ├── UserRepository.java      # Implements IRepository
│   ├── MediaRepository.java     # Implements IRepository
│   ├── RatingRepository.java    # Implements IRepository
│   └── FavoriteRepository.java  # Implements IRepository
│
├── service/
│   ├── IService.java            # Single template interface
│   ├── AuthService.java         # Implements IService
│   ├── MediaService.java        # Implements IService
│   ├── RatingService.java       # Implements IService
│   └── FavoriteService.java     # Implements IService
│
├── http/
│   ├── UserHandler.java         # User endpoints
│   ├── MediaHandler.java        # Media endpoints
│   ├── RatingHandler.java       # Rating endpoints
│   └── FavoriteHandler.java     # Favorites endpoints
│
└── util/
    ├── PasswordUtil.java        # PBKDF2 password hashing
    └── TokenService.java        # Bearer token management
```

---

## Tech Stack

- **Language**: Java 21 (with records, text blocks)
- **HTTP Server**: JDK HttpServer (no frameworks!)
- **Database**: PostgreSQL 15
- **Container**: Docker + Docker Compose
- **JSON**: Jackson 2.18.2
- **JDBC**: PostgreSQL JDBC Driver 42.7.2
- **Build**: Maven 3.8+
- **Testing**: Postman (integration), JUnit 5 (unit - in progress)

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    pw_hash TEXT NOT NULL,
    token TEXT
);

-- Media Entries
CREATE TABLE media_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    media_type TEXT NOT NULL,
    release_year INTEGER,
    genres TEXT,
    age_restriction INTEGER
);

-- Ratings
CREATE TABLE ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stars INTEGER NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(media_id, user_id)
);

-- Favorites
CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(user_id, media_id)
);
```

---

## Security Features

**Password Hashing**: PBKDF2 with SHA-256, 65536 iterations, random salt
**Token-based Auth**: Bearer tokens in format `userId;username;secret`
**SQL Injection Prevention**: PreparedStatements for all queries
**Ownership Verification**: Services verify user owns resource before update/delete
**CORS Support**: OPTIONS method handling for cross-origin requests

---

## Documentation

- **`protocol.md`** – Detailed technical documentation with architecture decisions
- **`README.md`** – This file, user guide and API reference
- **Postman Collection** – Complete API testing suite with examples
- **Code Comments** – JavaDoc-style comments throughout codebase

---

## Testing

### Integration Tests (Postman)

**Collection**: `postman/MRP_DYNAMIC_USER_VERSION.postman_collection.json`

Includes tests for:
- User registration and login
- Media CRUD operations
- Rating creation and deletion
- Favorites management
- Multi-user workflows
- Error handling scenarios

### Unit Tests (Required - In Progress)

**Target**: 20 meaningful unit tests covering:
- Password hashing and verification
- Token generation and parsing
- Input validation (stars 1-5, required fields)
- Repository CRUD operations
- Service business logic
- Authorization checks

**Framework**: JUnit 5 with Mockito

---

## SOLID Principles

### Single Responsibility Principle
- Handlers: HTTP concerns only
- Services: Business logic only
- Repositories: Data access only

### Open/Closed Principle
- Template interfaces define structure
- New entities added by implementing interfaces
- No modification of existing code

### Liskov Substitution Principle
- All repositories substitutable where IRepository expected
- All services substitutable where IService expected

### Interface Segregation Principle
- Template interfaces provide common operations
- Implementations use only needed methods
- Unused methods throw UnsupportedOperationException

### Dependency Inversion Principle
- High-level modules depend on abstractions (interfaces)
- Concrete implementations injected at runtime

---

## 🚦 HTTP Status Codes

### Success
- **200 OK** – GET, PUT successful
- **201 Created** – POST created resource
- **204 No Content** – DELETE successful

### Client Errors
- **400 Bad Request** – Invalid input
- **401 Unauthorized** – Missing/invalid token
- **403 Forbidden** – Insufficient permissions
- **404 Not Found** – Resource doesn't exist

### Server Errors
- **500 Internal Server Error** – Database/server error


---

##GitHub Repository

**Repository**: https://github.com/glenkishoti/mrp_project.git




