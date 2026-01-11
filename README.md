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

## Project Structure
```
src/main/java/at/fhtw/mrp/
├── Main.java                      # Application entry point with DI
│
├── config/
│   └── AppConfig.java             # Environment configuration
│
├── db/
│   └── Database.java              # JDBC connection management
│
├── model/
│   ├── User.java                  # User entity
│   ├── MediaEntry.java            # Media entity
│   ├── Rating.java                # Rating entity
│   └── Favorite.java              # Favorite entity
│
├── repo/
│   ├── IRepository.java           # Single template interface
│   ├── UserRepository.java        # Implements IRepository
│   ├── MediaRepository.java       # Implements IRepository
│   ├── RatingRepository.java      # Implements IRepository (with update())
│   └── FavoriteRepository.java    # Implements IRepository
│
├── service/
│   ├── IService.java              # Single template interface
│   ├── AuthService.java           # Implements IService
│   ├── MediaService.java          # Implements IService (with filter/sort/search)
│   ├── RatingService.java         # Implements IService (with update())
│   ├── FavoriteService.java       # Implements IService
│   └── UserProfileService.java    # NEW: User statistics and activity
│
├── http/
│   ├── UserHandler.java           # User endpoints
│   ├── MediaHandler.java          # Media endpoints (with filter/sort/search)
│   ├── RatingHandler.java         # Rating endpoints (with PUT)
│   ├── FavoriteHandler.java       # Favorites endpoints
│   └── UserProfileHandler.java    # NEW: Profile & statistics endpoints
│
└── util/
    ├── PasswordUtil.java          # PBKDF2 password hashing
    └── TokenService.java          # Bearer token management

src/test/java/at/fhtw/mrp/
├── service/
│   ├── AuthServiceTest.java      # 4 tests (register, login, duplicate, invalid)
│   ├── MediaServiceTest.java     # 5 tests (create, get, update, delete, ownership)
│   ├── RatingServiceTest.java    # 5 tests (create, validate, list, delete, ownership)
│   └── FavouriteServiceTest.java # 4 tests (add, list, check, remove)
│
└── util/
    └── TokenServiceTest.java      # 2 tests (generate, authenticate)

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

**Collection**: `postman/MRP.postman_collection.json`

Includes tests for:
- User registration and login
- Media CRUD operations
- Rating creation and deletion
- Favorites management
- Multi-user workflows
- Error handling scenarios

### Unit Tests (Required - In Progress)

**Target**: 20 meaningful unit tests covering:
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




