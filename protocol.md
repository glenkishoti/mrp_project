# **# Protocol – Media Ratings Platform (MRP)**

### 

### **## Overview**

Pure JDK HttpServer + JDBC + Jackson. No frameworks. PostgreSQL in Docker.

All identifiers now use **UUID (v7)** instead of integers.

Handlers are thin → Services contain business logic → Repositories handle SQL only (SOLID).

---

### **## Architecture decisions**

- **Layers**:

  - **http (handlers)** – routing, auth check, JSON parsing/writing  
  - **service** – validation, ownership rules, business logic  
  - **repo** – SQL access only (PreparedStatements), returns model objects  
  - **model** – POJOs  
  - **util/config** – PasswordUtil, TokenService, Database, AppConfig  

- **Auth**: Bearer token stored in `users.token`, validated through `TokenService.authenticate()`  
- **Password hashing**: PBKDF2 with salt (PasswordUtil)  
- **Serialization**: Jackson ObjectMapper (Map <-> JSON)  
- **DB**: `users`, `media_entries`, `ratings`  
- **UUID everywhere**, matching code & schema

---

### **## Data model**

```
users(
  id UUID PRIMARY KEY,
  username TEXT UNIQUE,
  pw_hash TEXT,
  token TEXT
)

media_entries(
  id UUID PRIMARY KEY,
  owner_id UUID REFERENCES users(id),
  title TEXT,
  description TEXT,
  media_type TEXT,
  release_year INT,
  genres TEXT,
  age_restriction INT
)

ratings(
  id UUID PRIMARY KEY,
  media_id UUID REFERENCES media_entries(id),
  user_id UUID REFERENCES users(id),
  stars INT,
  comment TEXT,
  created_at TIMESTAMP DEFAULT now()
)
```

---

### **## Class diagram (text)**

@startuml  
title MRP – Class Diagram (Handlers, Services, Repositories, Models)

##### ' ===== Models =====
class User {
  - id: UUID  
  - username: String  
  - pwHash: String  
  - token: String  
  --  
  + getId(): UUID  
  + getUsername(): String  
  + getPwHash(): String  
  + getToken(): String  
  + setToken(String): void  
}

class MediaEntry {
  - id: UUID  
  - ownerId: UUID  
  - title: String  
  - description: String  
  - mediaType: String  
  - releaseYear: Integer  
  - genres: String  
  - ageRestriction: Integer  
}

class Rating {
  - id: UUID  
  - mediaId: UUID  
  - userId: UUID  
  - stars: int  
  - comment: String  
  - createdAt: java.sql.Timestamp  
}

##### ' ===== Repository Interfaces =====
interface IUserRepository {
  + insert(User): void  
  + findByUsername(String): Optional<User>  
  + findById(UUID): Optional<User>  
  + updateToken(UUID,String): void  
}

interface IMediaRepository {
  + insert(MediaEntry): void  
  + findById(UUID): Optional<MediaEntry>  
  + findAll(String): List<MediaEntry>  
  + update(MediaEntry): void  
  + delete(UUID): void  
  + averageScore(UUID): Double  
}

interface IRatingRepository {
  + insert(Rating): void  
  + listByMedia(UUID): List<Rating>  
  + listByUser(UUID): List<Rating>  
  + findById(UUID): Optional<Rating>  
  + delete(UUID): void  
}

##### ' ===== Service Interfaces =====
interface IAuthService {
  + register(String,String): void  
  + login(String,String): Map<String,String>  
}

interface IMediaService {
  + create(UUID,String,String,String,Integer,String,Integer): UUID  
  + get(UUID): Optional<MediaEntry>  
  + list(String): List<MediaEntry>  
  + update(UUID,UUID,String,String,String,Integer,String,Integer): void  
  + delete(UUID,UUID): void  
  + averageScore(UUID): Double  
}

interface IRatingService {
  + create(UUID,UUID,int,String): UUID  
  + listByMedia(UUID): List<Rating>  
  + delete(UUID,UUID): void  
}

##### ' ===== HTTP Layer =====
class UserHandler
class MediaHandler
class RatingHandler

UserHandler --> IAuthService
MediaHandler --> IMediaService
MediaHandler --> IRatingService
RatingHandler --> IRatingService

AuthService --> IUserRepository
MediaService --> IMediaRepository
RatingService --> IRatingRepository
RatingService --> IMediaRepository

User "1" o-- "many" MediaEntry
User "1" o-- "many" Rating
MediaEntry "1" o-- "many" Rating

@enduml

---

### **## Key flows**

**Register**

1. UserHandler reads JSON  
2. AuthService.register → hash password → userRepo.insert  

**Login**

1. UserHandler reads JSON  
2. AuthService.login → verify PBKDF2 → generate token → updateToken  

**Media CRUD**

- Auth required  
- Ownership enforced in MediaService  
  `repo.update()` and `repo.delete()` operate only on existing IDs  

**Ratings**

- Creating/listing under: `/api/media/{id}/ratings`  
- Deleting under: `/api/ratings/{ratingId}`  
- Stars validated (1–5)  
- User can delete their own rating  

---

## **## HTTP codes**

- 201 Created  
- 200 OK  
- 204 No Content  
- 400 Invalid input  
- 401 Unauthorized  
- 403 Forbidden  
- 404 Not found  
- 500 DB/server error  

---

## **## Tests**

- Unit tests: password hashing, token parsing, service validation  
- Integration tests: Postman collection (login → media CRUD → ratings)  

---

## **## Problems & Solutions**

- UUID migration broke constructors → fixed all model classes  
- Repositories rewritten to accept UUID instead of int  
- Interfaces introduced to meet SOLID requirement  
- Handlers reduced; business logic moved to services  
- Postman collection updated  

---

## **## Time tracking (est.)**

- Auth: 3h  
- Media + Ratings: 5h  
- DB & Docker: 2h  
- Postman + Documentation: 2h  
