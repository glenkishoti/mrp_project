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
1. MediaHandler receives GET `/api/media` or GET `/api/media?query=term`
2. MediaService.list(query):
   - Calls MediaRepository.listByQuery() for ILIKE search
3. Returns 200 OK with media array

**Search by Title:**
1. MediaHandler receives GET `/api/media?search=term`
2. MediaService.searchByTitle(query):
   - Calls MediaRepository.listByQuery() for case-insensitive search
3. Returns 200 OK with matching media array

**Filter:**
1. MediaHandler receives GET `/api/media?genre=action&type=movie&minYear=2000&maxYear=2020&maxAge=16`
2. MediaService.filterAndSort(filters, sortBy, sortOrder):
   - Fetches all media
   - Applies Stream-based filters for genre, type, year range, age restriction
   - Can combine multiple filters
3. Returns 200 OK with filtered media array

**Sort:**
1. MediaHandler receives GET `/api/media?sortBy=title&sortOrder=asc`
2. MediaService.filterAndSort(filters, sortBy, sortOrder):
   - Applies Comparator-based sorting
   - Supports sorting by title, year, or score
   - Supports ascending or descending order
3. Returns 200 OK with sorted media array

**Get One:**
1. MediaHandler receives GET `/api/media/{id}`
2. MediaService.get(id):
   - Fetches media entry
   - Calculates average score
3. Returns 200 OK with media details

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
4. Returns 200 OK

### Ratings
**Create:**
1. RatingHandler receives POST `/api/ratings` with mediaId in body
2. TokenService validates token → gets User
3. RatingService.create():
   - Validates stars (1-5)
   - Creates rating with userId, mediaId
   - One rating per user per media (database constraint)
4. Returns 201 Created with rating ID

**Edit:**
1. RatingHandler receives PUT `/api/ratings/{ratingId}` with Bearer token
2. TokenService validates token → gets User
3. RatingService.update():
   - Verifies user is rating owner
   - Validates stars (1-5)
   - Updates rating stars and comment
4. Returns 200 OK

**List by Media:**
1. RatingHandler receives GET `/api/ratings?mediaId={id}` with Bearer token
2. TokenService validates token → gets User
3. RatingService.listByMedia(mediaId)
4. Returns 200 OK with ratings array

**List My Ratings:**
1. RatingHandler receives GET `/api/ratings` with Bearer token
2. TokenService validates token → gets User
3. RatingService.listByUser(userId)
4. Returns 200 OK with user's ratings array

**Delete:**
1. RatingHandler receives DELETE `/api/ratings/{ratingId}` with Bearer token
2. TokenService validates token → gets User
3. RatingService.delete():
   - Verifies user is rating owner
   - Deletes rating
4. Returns 200 OK

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

### User Profile & Statistics
**Get Profile:**
1. UserProfileHandler receives GET `/api/profile` with Bearer token
2. TokenService validates token → gets User
3. Returns 200 OK with user ID and username

**Get Statistics:**
1. UserProfileHandler receives GET `/api/profile/statistics` with Bearer token
2. TokenService validates token → gets User
3. UserProfileService.getUserStatistics():
   - Calculates totalRatingsGiven, averageScoreGiven
   - Counts totalMediaCreated, totalFavorites
   - Determines favoriteGenre (most common in favorites)
   - Identifies topGenres (top 3)
   - Calculates averageRatingReceived (on user's media)
4. Returns 200 OK with comprehensive statistics object

**Get Activity:**
1. UserProfileHandler receives GET `/api/profile/activity` with Bearer token
2. TokenService validates token → gets User
3. UserProfileService.getUserActivity():
   - Fetches mostRecentRating
   - Calculates ratingsDistribution (count by stars 1-5)
4. Returns 200 OK with activity data

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

### Unit Tests

## Unit Testing Strategy and Coverage

### Testing Approach

Created 20 comprehensive unit tests using **JUnit 5** and **Mockito** to validate core business logic in isolation. The testing strategy follows the **AAA pattern** (Arrange, Act, Assert) and uses mocking to isolate services from database dependencies.

### Test Framework and Tools

- **JUnit 5 (Jupiter)** - Modern testing framework with improved annotations and assertions
- **Mockito 5.5.0** - Mocking framework for creating test doubles
- **Maven Surefire Plugin** - Test execution and reporting

### Test Coverage (20 Tests Total)

#### 1. TokenServiceTest (3 tests)
Tests token generation and format validation without accessing private methods:
- **Test 1**: Token generation produces valid format (UUID;username;secret)
- **Test 2**: Two tokens for same user have different secrets (validates randomization)
- **Test 3**: Token contains valid UUID as first part

**Rationale**: Token security is critical for authentication. Tests verify format consistency and UUID validity without testing private implementation details.

#### 2. AuthServiceTest (5 tests)
Tests user authentication and registration workflows:
- **Test 4**: Register creates user and returns UUID
- **Test 5**: Register hashes password before storing (security validation)
- **Test 6**: Register creates unique user IDs
- **Test 7**: Authenticate with valid credentials returns token
- **Test 8**: Authenticate with wrong password throws IllegalArgumentException

**Rationale**: Authentication is the entry point to the system. Tests verify password hashing, token generation, and credential validation while mocking UserRepository to avoid database dependencies.

#### 3. FavoriteServiceTest (3 tests)
Tests favorite management business logic:
- **Test 9**: Add favorite with valid media succeeds
- **Test 10**: Add favorite with non-existent media throws IllegalArgumentException
- **Test 11**: Get user favorites returns correct list

**Rationale**: Favorites require media validation. Tests verify that only existing media can be favorited and that user favorites are retrieved correctly. Uses `doReturn()` for Mockito compatibility with wildcard return types.

#### 4. RatingServiceTest (5 tests)
Tests rating business rules and validation:
- **Test 12**: Create rating with valid stars (1-5) succeeds
- **Test 13**: Stars < 1 throws IllegalArgumentException
- **Test 14**: Stars > 5 throws IllegalArgumentException
- **Test 15**: Null comment succeeds (comment is optional)
- **Test 16**: Empty comment succeeds

**Rationale**: The specification requires stars to be 1-5. These tests enforce this critical business rule and verify that comments are optional. Exception messages are validated to ensure proper error reporting.

#### 5. MediaServiceTest (4 tests)
Tests media creation and UUID generation:
- **Test 17**: Create media with valid fields succeeds
- **Test 18**: Create media with minimal fields succeeds
- **Test 19**: Create media with null optional fields succeeds
- **Test 20**: Create media returns unique IDs

**Rationale**: Media entries are the core entities. Tests verify successful creation with various field combinations and UUID uniqueness. Note: Input validation is handled at the handler layer, so service tests focus on successful operations.

### Testing Patterns and Best Practices

#### Mocking Strategy
- **What we mock**: All repository dependencies (UserRepository, MediaRepository, RatingRepository, FavoriteRepository)
- **Why**: To isolate service logic from database layer and avoid database setup in unit tests
- **How**: Using Mockito's `@Mock` annotation and `MockitoAnnotations.openMocks()`

#### Test Structure (AAA Pattern)
```java
@Test
void testExample() {
    // Arrange - Set up test data and mocks
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.of(entity));
    
    // Act - Execute the method under test
    Result result = service.someMethod(id);
    
    // Assert - Verify the outcome
    assertNotNull(result);
    verify(repository).findById(id);
}
```

#### Key Testing Decisions

1. **No Database Access**: All tests use mocks instead of a test database, ensuring fast execution and no setup requirements
2. **Focus on Business Logic**: Tests validate business rules (stars 1-5, media existence) rather than data persistence
3. **Mockito doReturn() for Wildcards**: Used `doReturn().when()` instead of `when().thenReturn()` for methods returning `Optional<?>` to avoid type inference issues
4. **Exception Testing**: Validates both exception types and messages for proper error handling
5. **Concrete Repository Classes**: Tests mock concrete repository classes (UserRepository, MediaRepository, etc.) that implement the unified IRepository interface

### Integration Tests

**Postman Collection**: `MRP.postman_collection.json`

Test flows:
**Users (3 tests):**
1. Register user
2. Login (auto-saves token + username)
3. Get user profile (GET /api/users/{username}/profile)

**Media (6 tests):**
4. Create media (auto-saves mediaId)
5. List all media
6. Search media by title (GET /api/media?query=...)
7. Get one media
8. Update media
9. Delete media

**Media (Search & Filter) (6 tests):**
10. Search by title (GET /api/media?search=query)
11. Filter by genre (GET /api/media?genre=sci-fi)
12. Filter by type (GET /api/media?type=movie)
13. Filter by year range (GET /api/media?minYear=2000&maxYear=2020)
14. Filter by age restriction (GET /api/media?maxAge=16)
15. Combined filters (genre + type + year + age)

**Media (Sort) (5 tests):**
16. Sort by title ascending (GET /api/media?sortBy=title&sortOrder=asc)
17. Sort by title descending (GET /api/media?sortBy=title&sortOrder=desc)
18. Sort by year newest first (GET /api/media?sortBy=year&sortOrder=desc)
19. Sort by year oldest first (GET /api/media?sortBy=year&sortOrder=asc)
20. Filter and sort combined (GET /api/media?genre=action&sortBy=year&sortOrder=desc)

**Ratings (5 tests):**
21. Create rating (auto-saves ratingId)
22. List ratings for media (GET /api/ratings?mediaId={id})
23. Edit rating (PUT /api/ratings/{id})
24. Get my ratings (GET /api/ratings)
25. Delete rating

**Favorites (4 tests):**
26. Add media to favorites
27. Get user's favorites
28. Check if favorited
29. Remove from favorites

**Profile & Statistics (3 tests):**
30. Get profile (GET /api/profile)
31. Get statistics (GET /api/profile/statistics)
32. Get activity (GET /api/profile/activity)

**Total**: 32 integration tests across 7 feature folders

**Environment**: `MRP_Local.postman_environment.json`
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

### Testing & Quality Assurance
- **Unit Testing**: 4 hours
  - Created 20 JUnit tests with Mockito
  - Mocked all repository dependencies
  - Tested UserService, MediaService, RatingService, FavoriteService
  - Achieved comprehensive coverage of CRUD operations
  - Fixed interface mocking issues with concrete types

### New Feature Implementation
- **Edit Ratings Feature**: 3 hours
  - Implemented RatingService.update() with ownership validation
  - Added PUT endpoint in RatingHandler
  - Updated RatingRepository with SQL UPDATE
  - Fixed path parsing bugs (parts[2] → parts[3])
  
- **Search & Filter Media**: 4 hours
  - Implemented MediaService.searchByTitle() for case-insensitive search
  - Created MediaService.filterAndSort() with Stream-based filtering
  - Added support for genre, type, year range, age restriction filters
  - Enabled multiple filter combinations
  
- **Sort Results Feature**: 2 hours
  - Implemented Comparator-based sorting by title, year, score
  - Added ascending/descending order support
  - Integrated sorting with filtering
  
- **User Profile Statistics**: 3 hours
  - Created UserProfileService with comprehensive statistics
  - Calculated total ratings, averages, favorite genres
  - Implemented ratings distribution and recent activity
  - Added 3 new endpoints for profile, statistics, and activity
  
- **Postman Testing Collection**: 2 hours
  - Extended existing collection with 4 new folders (17 requests)
  - Added automated tests for all new features
  - Created dynamic test workflows
  - Fixed endpoint URLs to match new handler structure

**Total Project Time**: ~40 hours

---

## SOLID Principles Applied

### Single Responsibility Principle (SRP)
Each class has one clear responsibility:

**Example 1: RatingHandler vs RatingService vs RatingRepository**
- `RatingHandler` - Only handles HTTP: parses requests, authenticates users, sends responses
- `RatingService` - Only handles business logic: validates stars (1-5), manages approval status
- `RatingRepository` - Only handles database: SQL queries, result mapping

**Example 2: Comment Approval Separation**
- `RatingService.create()` - Creates rating with "pending" status
- `RatingService.approveRating()` - Separate method for approval workflow
- Each method has one job, making testing and maintenance easier

### Dependency Inversion Principle (DIP)
High-level modules depend on abstractions, not concrete classes:

**Example: Service Layer Dependencies**
```java
// RatingService depends on IRepository interface, not concrete class
public class RatingService implements IService {
    private final RatingRepository ratingRepository; // Uses concrete type for custom methods
    
    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }
}
```

**Example: Main.java Dependency Injection**
```java
// Main.java creates concrete implementations
RatingRepository ratingRepo = new RatingRepository();
RatingService ratingService = new RatingService(ratingRepo);
RatingHandler ratingHandler = new RatingHandler(ratingService, ...);

// Handlers depend on services, services depend on repositories
// Easy to swap implementations for testing or different databases
```

This allows mock repositories in unit tests and makes the system flexible for changes.

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
- Step 4: Added 20 Unit Tests
- Step 5: Added new features: edit ratings, search media by title, filter media, sort results, user stats
- Step 6: Added Comment requiring confirmation functionality + last touches

---

## Conclusion

The MRP project successfully implements a RESTful API server using pure Java without frameworks. The architecture follows SOLID principles with a revolutionary simplification from 8 to 2 template interfaces using the Template Method Pattern. All CRUD operations work correctly with proper authentication, authorization, and data validation. The dynamic Postman collection enables easy testing with multiple users. The system is ready for the next phase: comprehensive unit testing and advanced features like search/filter, user profiles, and recommendations.