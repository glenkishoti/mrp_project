# **# Protocol – Media Ratings Platform (MRP)**

### 

### **## Overview**

Pure JDK HttpServer + JDBC + Jackson. No frameworks as required. PostgreSQL in Docker.



#### **## Architecture decisions**

\- \*\*Layers\*\*:

&nbsp; - http (thin handlers) – routing, auth check, JSON IO

&nbsp; - service – business rules (validation, ownership)

&nbsp; - repo – SQL only via PreparedStatements

&nbsp; - model – POJOs

&nbsp; - db/config/util – infrastructure

\- \*\*Auth\*\*: token-based (`Authorization: Bearer <token>`), stored in users.token.

\- \*\*Password\*\*: PBKDF2 with salt in `PasswordUtil`.

\- \*\*Serialization\*\*: Jackson ObjectMapper (request → Map, response → Map/POJO).

\- \*\*DB\*\*: users, media\_entries, ratings (+ rating\_likes for later).



#### **## Data model**



users(id, username, password\_hash, token)



media\_entries(

&nbsp; id, owner\_id -> users.id, title, description, media\_type('movie'|'series'|'game'),

&nbsp; release\_year, genres, age\_restriction, created\_at

)



ratings(

&nbsp; id, media\_id -> media\_entries.id, user\_id -> users.id,

&nbsp; stars(1..5), comment, comment\_confirmed, created\_at,

&nbsp; UNIQUE(media\_id, user\_id)

)



#### **## Class diagram (text)**



@startuml

title MRP – Class Diagram (Handlers, Services, Repositories, Models)





##### ' ===== Models =====

class User {

&nbsp; - id: int

&nbsp; - username: String

&nbsp; - passwordHash: String

&nbsp; - token: String

&nbsp; -- 

&nbsp; + getId(): int

&nbsp; + getUsername(): String

&nbsp; + getPasswordHash(): String

&nbsp; + getToken(): String

&nbsp; + setToken(token: String): void

}



class MediaEntry {

&nbsp; - id: int

&nbsp; - ownerId: int

&nbsp; - title: String

&nbsp; - description: String

&nbsp; - mediaType: String

&nbsp; - releaseYear: Integer

&nbsp; - genres: String

&nbsp; - ageRestriction: Integer

&nbsp; --

&nbsp; + getId(): int

&nbsp; + getOwnerId(): int

&nbsp; + getTitle(): String

&nbsp; + getDescription(): String

&nbsp; + getMediaType(): String

&nbsp; + getReleaseYear(): Integer

&nbsp; + getGenres(): String

&nbsp; + getAgeRestriction(): Integer

}



class Rating {

&nbsp; - id: int

&nbsp; - mediaId: int

&nbsp; - userId: int

&nbsp; - stars: int

&nbsp; - comment: String

&nbsp; - createdAt: java.sql.Timestamp

&nbsp; --

&nbsp; + getId(): int

&nbsp; + getMediaId(): int

&nbsp; + getUserId(): int

&nbsp; + getStars(): int

&nbsp; + getComment(): String

&nbsp; + getCreatedAt(): java.sql.Timestamp

}



##### ' ===== Repositories =====

class UserRepository {

&nbsp; + insert(username: String, passwordHash: String): void

&nbsp; + findByUsername(username: String): Optional<User>

&nbsp; + findByToken(token: String): Optional<User>

&nbsp; + updateToken(username: String, token: String): void

}



class MediaRepository {

&nbsp; + insert(ownerId:int, title:String, description:String, type:String, year:Integer, genres:String, age:Integer): int

&nbsp; + findById(id:int): Optional<MediaEntry>

&nbsp; + list(query:String): List<MediaEntry>

&nbsp; + update(id:int, ownerId:int, title:String, description:String, type:String, year:Integer, genres:String, age:Integer): void

&nbsp; + delete(id:int, ownerId:int): void

&nbsp; + averageScore(mediaId:int): Double

}



class RatingRepository {

&nbsp; + insert(mediaId:int, userId:int, stars:int, comment:String): int

&nbsp; + listByMedia(mediaId:int): List<Map<String,Object>>

&nbsp; + findById(id:int): Optional<Rating>

&nbsp; + update(id:int, userId:int, stars:int, comment:String): void

&nbsp; + delete(id:int, userId:int): void

}



##### ' ===== Services =====

class AuthService {

&nbsp; - users: UserRepository

&nbsp; --

&nbsp; + register(username:String, password:String): void

&nbsp; + login(username:String, password:String): Map<String,String>  ' { "token": ... }

}



class MediaService {

&nbsp; - repo: MediaRepository

&nbsp; --

&nbsp; + create(ownerId:int, title:String, desc:String, type:String, year:Integer, genres:String, age:Integer): int

&nbsp; + get(id:int): Optional<MediaEntry>

&nbsp; + list(query:String): List<MediaEntry>

&nbsp; + update(id:int, ownerId:int, title:String, desc:String, type:String, year:Integer, genres:String, age:Integer): void

&nbsp; + delete(id:int, ownerId:int): void

&nbsp; + averageScore(mediaId:int): Double

}



class RatingService {

&nbsp; - repo: RatingRepository

&nbsp; - mediaRepo: MediaRepository

&nbsp; --

&nbsp; + create(mediaId:int, userId:int, stars:int, comment:String): int

&nbsp; + listByMedia(mediaId:int): List<Map<String,Object>>

&nbsp; + update(ratingId:int, userId:int, stars:int, comment:String): void

&nbsp; + delete(ratingId:int, userId:int): void

}



##### ' ===== HTTP Layer =====

class UserHandler {

&nbsp; - mapper: com.fasterxml.jackson.databind.ObjectMapper

&nbsp; - auth: AuthService

&nbsp; --

&nbsp; + handle(ex: com.sun.net.httpserver.HttpExchange): void

&nbsp; - handleRegister(ex): void

&nbsp; - handleLogin(ex): void

&nbsp; - handleProfile(ex): void

&nbsp; - send(ex, code:int, payload:Object): void

}



class MediaHandler {

&nbsp; - mapper: com.fasterxml.jackson.databind.ObjectMapper

&nbsp; - media: MediaService

&nbsp; - ratings: RatingService

&nbsp; - users: UserRepository

&nbsp; --

&nbsp; + handle(ex: com.sun.net.httpserver.HttpExchange): void

&nbsp; ' helpers: readJson, parseQuery, extractId, requireAuth, send

}



class RatingHandler {

&nbsp; - mapper: com.fasterxml.jackson.databind.ObjectMapper

&nbsp; - ratings: RatingService

&nbsp; - users: UserRepository

&nbsp; --

&nbsp; + handle(ex: com.sun.net.httpserver.HttpExchange): void

}



##### ' ===== Utilities =====

class TokenService {

&nbsp; + authenticate(authzHeader:String, users:UserRepository): Optional<User>

&nbsp; + randomToken(): String

}



##### ' ===== Wiring / Associations =====

UserHandler --> AuthService

MediaHandler --> MediaService

MediaHandler --> RatingService

MediaHandler --> UserRepository

RatingHandler --> RatingService

RatingHandler --> UserRepository



AuthService --> UserRepository

MediaService --> MediaRepository

RatingService --> RatingRepository

RatingService --> MediaRepository



MediaEntry "1" o-- "many" Rating : receives

User "1" o-- "many" MediaEntry : owns

User "1" o-- "many" Rating : writes



@enduml



#### **## Key flows**



\*\*Register\*\*

1\) UserHandler.read JSON

2\) AuthService.register → UserRepository.insert



\*\*Login\*\*

1\) UserHandler.read JSON

2\) AuthService.login → verify PBKDF2 → token → UserRepository.updateToken



\*\*Media CRUD\*\*

\- Create/Update/Delete require token. Ownership enforced in MediaService by repo update/delete WHERE owner\_id=?



\*\*Ratings\*\*

\- Create/list under /api/media/{id}/ratings; update/delete via /api/ratings/{ratingId}

\- Stars validated 1..5; update/delete only by author.



## **## HTTP codes**

\- 201 Created on create

\- 200 OK on get/update

\- 204 No Content on delete

\- 400 invalid input

\- 401/403 unauthorized/forbidden

\- 404 not found

\- 500 db/server error



## **## Tests**

\- Unit: password hash verify, token parsing, MediaService validation, RatingService validation.

\- Integration: Postman collection (register, login, media CRUD, ratings path).



## **## Problems \& Solutions**

\- Docker Desktop engine issues → reset WSL, verified with `docker version`.

\- Postgres init ordering → applied schema via `docker exec ... -f`.



## **## Time tracking (est.)**

\- Users (auth): 3h

\- Media \& ratings: 5h

\- DB \& Docker: 2h

\- Postman \& docs: 2h





