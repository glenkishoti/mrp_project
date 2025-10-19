# **# MRP – Media Ratings Platform**



Small HTTP server in \*\*Java 21\*\* using only the JDK `HttpServer` (no Spring/JSP/ASP).  

Persistence: PostgreSQL (Docker). JSON: Jackson.

### 

### **## Run**



#### **- Start DB (Docker):**

&nbsp; docker compose up -d

&nbsp; docker compose ps



#### **- Apply schema (first time):**

&nbsp; psql inside container loads files in db/init/\*.sql automatically on first run.

&nbsp; If you added 002\_media.sql later:

&nbsp;   docker cp db/init/002\_media.sql mrp\_postgres:/tmp/002\_media.sql

&nbsp;   docker exec -it mrp\_postgres psql -U mrp\_user -d mrp\_db -f /tmp/002\_media.sql



#### **- Start server:**

&nbsp; mvn clean compile

&nbsp; Run `Main` from IntelliJ, or:

&nbsp; mvn package

&nbsp; java -cp target/classes at.fhtw.mrp.Main



Server prints: `MRP server running at http://localhost:8080`



### **## Endpoints**

### 

#### **### Users**

\- POST /api/users/register  – `{ "username", "password" }` → 201

\- POST /api/users/login     – `{ "Username", "Password" }` → `{ "token": "..." }`

&nbsp; Send token as `Authorization: Bearer <token>` on protected routes.



#### **### Media**

\- GET  /api/media?query=term          – list (filter by title)

\- POST /api/media                     – create (auth)

\- GET  /api/media/{id}                – get one + average score

\- PUT  /api/media/{id}                – update (owner only)

\- DELETE /api/media/{id}              – delete (owner only)



#### **### Ratings**

\- GET  /api/media/{id}/ratings        – list ratings for media

\- POST /api/media/{id}/ratings        – create rating (auth)

\- PUT  /api/ratings/{ratingId}        – update own rating

\- DELETE /api/ratings/{ratingId}      – delete own rating



#### **## Postman**

Environment:

\- baseUrl = http://localhost:8080

\- token   = (filled by login)



Collection includes Register → Login → Media CRUD → Ratings.



### **## Tech Stack \& Structure**

\- config: AppConfig (env → JDBC URL)

\- db: Database (JDBC connection)

\- model: User, MediaEntry, Rating

\- repo: UserRepository, MediaRepository, RatingRepository (JDBC)

\- service: AuthService, MediaService, RatingService (business rules)

\- http: UserHandler, MediaHandler, RatingHandler (routing + JSON)

\- util: PasswordUtil (PBKDF2), TokenService



### **## API \& Tests**

\- Postman collection: `postman/MRP\_Postman\_Collection.json`

\- cURL demo: `scripts/demo.sh` / `scripts/demo.cmd`



### **## GitHub**

Repo: https://github.com/glenkishoti/mrp\_project.git   ← update after first push



