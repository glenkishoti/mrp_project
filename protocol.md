# Protocol – Media Ratings Platform (MRP)

## Overview

Pure JDK HttpServer + JDBC + Jackson. No frameworks. PostgreSQL in Docker.

All identifiers use **UUID (v7)** instead of integers.

**Architecture**: Handlers (thin) → Services (business logic) → Repositories (SQL only) following SOLID principles.

**Key Achievement**: Simplified from 8 separate interfaces to **2 unified template interfaces** using Template Method Pattern.

---

## Architecture Decisions

### Layer Structure

- **http (handlers)** – HTTP routing, authentication checks, JSON parsing/serialization
- **service** – Business logic, validation, ownership rules, authorization
- **repo** – Data access layer with SQL only (PreparedStatements), returns model objects
- **model** – POJOs (Plain Old Java Objects)
- **util/config** – PasswordUtil, TokenService, Database

### Interface Architecture


1. **IRepository** – Single template interface for ALL repositories
   - Defines common operations: insert, findById, update, delete, listAll, listByQuery, listByRelatedId, findByString
   - Each repository implements this interface
   - Unused methods throw `UnsupportedOperationException`

2. **IService** – Single template interface for ALL services
   - Defines common operations: create, get, list, listByUser, update, delete, authenticate, register
   - Each service implements this interface
   - Unused methods throw `UnsupportedOperationException`

**Benefits**:
- Clear contract of available operations
- Follows Template Method Pattern
- DRY principle: common operations defined once
- Easier to add new entities

### Authentication & Security

- **Auth**: Bearer token stored in `users.token`, validated through `TokenService.authenticate()`
- **Password hashing**: PBKDF2 with salt (PasswordUtil)
- **Token format**: `userId;username;secret` (e.g., `b264b73b-5b02-4caa-9279-e827af3de9e8;alice;c13aebac-ef73-4a91-918b-79e70c063be1`)
- **Authorization**: Ownership verification in services before update/delete operations

### Data Serialization

- **Jackson ObjectMapper** for JSON ↔ Java object conversion
- Maps used for flexible request/response handling

### Database

- **PostgreSQL** running in Docker
- Connection pooling via JDBC
- Tables: `users`, `media_entries`, `ratings`, `favorites`
- **UUID everywhere** for all primary keys and foreign keys

---

## Data Model

### Database Schema

```sql
-- Users table
users(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username TEXT UNIQUE NOT NULL,
  pw_hash TEXT NOT NULL,
  token TEXT
);

-- Media entries table
media_entries(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  description TEXT,
  media_type TEXT NOT NULL,
  release_year INTEGER,
  genres TEXT,
  age_restriction INTEGER
);

-- Ratings table
ratings(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  stars INTEGER NOT NULL CHECK (stars >= 1 AND stars <= 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE(media_id, user_id)  -- One rating per user per media
);

-- Favorites table
favorites(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  media_id UUID NOT NULL REFERENCES media_entries(id) ON DELETE CASCADE,
  created_at TIMESTAMP DEFAULT now(),
  UNIQUE(user_id, media_id)  -- One favorite per user per media
);

-- Trigger to update average rating when ratings change
CREATE OR REPLACE FUNCTION update_media_avg_rating()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE media_entries
    SET avg_rating = (
        SELECT AVG(stars)::DECIMAL(3,2)
        FROM ratings
        WHERE media_id = COALESCE(NEW.media_id, OLD.media_id)
    )
    WHERE id = COALESCE(NEW.media_id, OLD.media_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Requests                        │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┼────────────┬──────────────┬─────────────┐
        ▼            ▼            ▼              ▼             ▼
┌──────────────┬──────────────┬──────────────┬──────────────┬──────────────┐
│UserHandler   │MediaHandler  │RatingHandler │FavoriteHandler│             │
│  (HTTP)      │   (HTTP)     │   (HTTP)     │    (HTTP)     │             │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬────────┴─────────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌──────────────┬──────────────┬──────────────┬──────────────┐
│AuthService   │MediaService  │RatingService │FavoriteService│
│implements    │implements    │implements    │implements     │
│IService      │IService      │IService      │IService       │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬────────┘
       │              │              │              │
       ▼              ▼              ▼              ▼
┌──────────────┬──────────────┬──────────────┬──────────────┐
│UserRepo      │MediaRepo     │RatingRepo    │FavoriteRepo  │
│implements    │implements    │implements    │implements    │
│IRepository   │IRepository   │IRepository   │IRepository   │
└──────┬───────┴──────┬───────┴──────┬───────┴──────┬────────┘
       │              │              │              │
       └──────────────┴──────────────┴──────────────┘
                           │
                           ▼
                 ┌──────────────────┐
                 │   PostgreSQL     │
                 │    Database      │
                 └──────────────────┘
```


---

## Key Flows

### User Registration

1. UserHandler receives POST `/api/users/register`
2. Validates JSON body (username, password)
3. AuthService.register():
   - Hashes password with PBKDF2
   - Calls UserRepository.insert()
4. Returns 201 Created

### User Login

1. UserHandler receives POST `/api/users/login`
2. Validates JSON body (Username, Password)
3. AuthService.authenticate():
   - Finds user by username
   - Verifies password hash
   - Generates new token
   - Updates token in database
4. Returns 200 OK with token

### Media CRUD

**Create:**
1. MediaHandler receives POST `/api/media` with Bearer token
2. TokenService validates token → gets User
3. MediaService.create():
   - Validates fields (title, mediaType, etc.)
   - Sets ownerId to current user
   - Calls MediaRepository.insert()
4. Returns 201 Created with media ID

**List/Search:**
1. MediaHandler receives GET `/api/media?query=term`
2. MediaService.list(query):
   - Calls MediaRepository.listByQuery() for ILIKE search
3. Returns 200 OK with media array

**Update:**
1. MediaHandler receives PUT `/api/media/{id}` with Bearer token
2. TokenService validates token → gets User
3. MediaService.update():
   - Verifies user is owner
   - Updates media entry
4. Returns 200 OK

**Delete:**
1. MediaHandler receives DELETE `/api/media/{id}` with Bearer token
2. TokenService validates token → gets User
3. MediaService.delete():
   - Verifies user is owner
   - Calls MediaRepository.delete()
4. Returns 204 No Content

### Ratings

**Create:**
1. MediaHandler receives POST `/api/media/{mediaId}/ratings`
2. TokenService validates token → gets User
3. RatingService.create():
   - Validates stars (1-5)
   - Creates rating with userId, mediaId
   - One rating per user per media (database constraint)
4. Returns 201 Created with rating ID

**List:**
1. MediaHandler receives GET `/api/media/{mediaId}/ratings`
2. RatingService.listByMedia(mediaId)
3. Returns 200 OK with ratings array

**Delete:**
1. RatingHandler receives DELETE `/api/ratings/{ratingId}`
2. TokenService validates token → gets User
3. RatingService.delete():
   - Verifies user is rating owner
   - Deletes rating
4. Returns 204 No Content

### Favorites

**Add to Favorites:**
1. FavoriteHandler receives POST `/api/favorites` with mediaId in body
2. TokenService validates token → gets User
3. FavoriteService.addFavorite():
   - Validates media exists
   - Creates favorite entry
   - One favorite per user per media (database constraint)
4. Returns 201 Created

**Get User's Favorites:**
1. FavoriteHandler receives GET `/api/favorites`
2. TokenService validates token → gets User
3. FavoriteService.getUserFavorites():
   - Returns list of MediaEntry objects user has favorited
4. Returns 200 OK with media array

**Check if Favorited:**
1. FavoriteHandler receives GET `/api/favorites/{mediaId}/status`
2. TokenService validates token → gets User
3. FavoriteService.isFavorite(userId, mediaId)
4. Returns 200 OK with `{"isFavorite": true/false}`

**Remove from Favorites:**
1. FavoriteHandler receives DELETE `/api/favorites/{mediaId}`
2. TokenService validates token → gets User
3. FavoriteService.removeFavorite()
4. Returns 200 OK

---

## HTTP Status Codes

### Success (2xx)
- **200 OK** – Successful GET, PUT, or operation with response body
- **201 Created** – Successful POST creating new resource (includes ID in response)
- **204 No Content** – Successful DELETE with no response body

### Client Errors (4xx)
- **400 Bad Request** – Invalid input, missing fields, validation errors
- **401 Unauthorized** – Missing or invalid Bearer token
- **403 Forbidden** – Valid token but user lacks permission (not owner)
- **404 Not Found** – Resource doesn't exist

### Server Errors (5xx)
- **500 Internal Server Error** – Database errors, unexpected exceptions

---

## Testing

### Unit Tests (Not yet implemented - MUST-HAVE requirement!)

Required test coverage:
- PasswordUtil: hashing, verification
- TokenService: token generation, parsing, authentication
- Service validation: stars validation (1-5), field validation
- Repository operations: CRUD operations
- Authorization checks: ownership verification

**Total required: 20 meaningful unit tests**

### Integration Tests

**Postman Collection**: `MRP_DYNAMIC_USER_VERSION.postman_collection.json`

Test flows:
1. Register user
2. Login (auto-saves token + username)
3. Create media (auto-saves mediaId)
4. Get media
5. Update media
6. Create rating (auto-saves ratingId)
7. List ratings
8. Add to favorites
9. Get favorites
10. Delete rating
11. Delete media

**Environment**: `MRP_Local_FIXED.postman_environment.json`
- `baseUrl`: http://localhost:8080
- `username`: Auto-filled on login
- `token`: Auto-filled on login
- `mediaId`: Auto-filled on media creation
- `ratingId`: Auto-filled on rating creation

**Dynamic User Support**: The collection now works for ANY user, not just Alice. Simply login with different credentials and all subsequent requests use that user's context.

---

## Problems & Solutions

### Problem 1: UUID Migration
**Issue**: Initial design used integer IDs, needed to migrate to UUID for better scalability and security.

**Solution**: 
- Updated all model classes to use UUID
- Rewrote all repositories to handle UUID
- Updated database schema with gen_random_uuid()
- Updated Postman collection to handle UUID strings

### Problem 2: Interface Proliferation
**Issue**: Had 8 separate interfaces (IUserRepository, IMediaRepository, IRatingRepository, IFavoriteRepository, IAuthService, IMediaService, IRatingService, IFavoriteService) leading to code duplication and maintenance overhead.

**Solution**:
- Created single IRepository template interface
- Created single IService template interface
- Applied Template Method Pattern
- Reduced interfaces by 75% while maintaining SOLID principles
- Unused methods throw UnsupportedOperationException

### Problem 3: Hardcoded Postman Collection
**Issue**: Postman collection was hardcoded for "alice" user with `aliceUsername` variable, making multi-user testing difficult.

**Solution**:
- Created dynamic collection using `{{username}}` variable
- Added Test scripts to auto-save token, username, mediaId, ratingId on login/creation
- Fixed environment variables to be user-agnostic
- Users can now switch contexts by simply logging in again

### Problem 4: Type Casting in TokenService
**Issue**: `IRepository.findById()` returns `Optional<?>` but needed `Optional<User>` for type safety.

**Solution**:
- Used explicit type casting: `(User) optionalUser.get()`
- Safe because UserRepository only works with User entities
- Compiler satisfied, runtime safe

### Problem 5: Services Need Repository Helper Methods
**Issue**: Services needed access to repository-specific methods not in IRepository interface (e.g., `averageScore()`, `listByMedia()`, `findByUsername()`).

**Solution**:
- Services use concrete repository types (e.g., `MediaRepository`) instead of interface
- Maintains access to helper methods beyond template
- Handlers remain thin and use concrete service types
- Template interfaces provide structure, concrete types provide functionality

---

## Time Tracking

### Initial Development
- **Authentication & Users**: 3 hours
  - Password hashing with PBKDF2
  - Token generation and validation
  - User registration and login endpoints

- **Media & Ratings**: 5 hours
  - Media CRUD operations
  - Ownership verification
  - Ratings with star validation
  - Average score calculation

- **Database & Docker**: 2 hours
  - PostgreSQL setup
  - Schema design with UUID
  - Docker Compose configuration
  - Connection pooling

- **Favorites Feature**: 2 hours
  - Database schema with favorites table
  - FavoriteRepository and FavoriteService
  - FavoriteHandler with 4 endpoints
  - Integration with existing media system

### Refactoring & Architecture Improvements
- **Interface Simplification**: 4 hours
  - Designed IRepository and IService templates
  - Refactored all repositories to implement IRepository
  - Refactored all services to implement IService
  - Updated all handlers for concrete types
  - Fixed TokenService type casting issues

- **Postman Collection Improvement**: 2 hours
  - Created dynamic user-agnostic collection
  - Added Test scripts for auto-saving variables
  - Fixed environment variables
  - Tested multi-user workflows

- **Documentation**: 2 hours
  - Updated protocol.md and README.md
  - Created comprehensive guides
  - Documented architecture decisions

**Total Estimated Time**: ~20 hours

---

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
- **Handlers**: Only handle HTTP concerns (routing, parsing, status codes)
- **Services**: Only contain business logic and validation
- **Repositories**: Only handle database operations
- **Models**: Only data containers

### Open/Closed Principle (OCP)
- Template interfaces (IRepository, IService) define operations
- New entities can be added by implementing interfaces
- No modification of existing code needed

### Liskov Substitution Principle (LSP)
- All repositories can be used where IRepository is expected
- All services can be used where IService is expected
- UnsupportedOperationException for inapplicable operations is acceptable

### Interface Segregation Principle (ISP)
- Template interfaces provide common operations
- Implementations only use methods they need
- Unused methods throw UnsupportedOperationException rather than null implementation

### Dependency Inversion Principle (DIP)
- Services depend on repository interfaces
- Handlers depend on service interfaces
- Main.java wires concrete implementations

---

## Git Repository

**GitHub**: https://github.com/glenkishoti/mrp_project.git

Branches:
- `main`: Production-ready code with UUID implementation
- `feature/*`: Feature development branches

Commit history shows:
- Initial: server + auth + DB + docs/postman/curl
- Step 2: Feature-> media + rating handlers + SQL schema update
- Fix handler constructors, inject services properly, correct Main wiring
- Step 3: Added Media Favourite functionality, IRepository and IService interfaces and fixed Postman integration tests to work dynamically

---

## Conclusion

The MRP project successfully implements a RESTful API server using pure Java without frameworks. The architecture follows SOLID principles with a revolutionary simplification from 8 to 2 template interfaces using the Template Method Pattern. All CRUD operations work correctly with proper authentication, authorization, and data validation. The dynamic Postman collection enables easy testing with multiple users. The system is ready for the next phase: comprehensive unit testing and advanced features like search/filter, user profiles, and recommendations.