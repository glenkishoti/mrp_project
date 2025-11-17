# **# MRP – Media Ratings Platform**

Small HTTP server in **Java 21** using only the JDK `HttpServer`.  
Persistence: PostgreSQL (Docker). JSON via Jackson.

All IDs use **UUID**.  
Architecture follows SOLID: Handlers → Services → Repositories → Database.

---

### **## Run**

#### **- Start DB (Docker):**
```
docker compose up -d
docker compose ps
```

#### **- Apply schema (first time):**

All SQL is in `db/init/`.

When the container starts for the first time, it auto-loads all `*.sql`.

If you add new scripts later:

```
docker cp db/init/001_schema.sql mrp_postgres:/tmp/schema.sql
docker exec -it mrp_postgres psql -U mrp_user -d mrp_db -f /tmp/schema.sql
```

---

#### **- Start server**
```
mvn clean compile
mvn package
java -cp target/classes at.fhtw.mrp.Main
```

Server will print:

```
MRP server running at http://localhost:8080
```

---

### **## Endpoints**

### **### Users**
```
POST /api/users/register
POST /api/users/login
GET  /api/users/profile (requires Bearer token)
```

### **### Media**
```
GET    /api/media?query=term
POST   /api/media
GET    /api/media/{id}
PUT    /api/media/{id}
DELETE /api/media/{id}
```

### **### Ratings**
```
GET    /api/media/{id}/ratings
POST   /api/media/{id}/ratings
DELETE /api/ratings/{ratingId}
```

All create/update/delete operations require **Authorization: Bearer <token>**.

---

### **## Postman**

Environment variables:

```
baseUrl = http://localhost:8080
token   = <filled after login>
```

Collection includes:

1. Register  
2. Login  
3. Media CRUD  
4. Ratings  


---

### **## Tech Stack & Structure**

- **config**: AppConfig (env → JDBC)  
- **db**: Database (JDBC)  
- **model**: User, MediaEntry, Rating  
- **repo**: IUserRepository, IMediaRepository, IRatingRepository (+ implementations)  
- **service**: IAuthService, IMediaService, IRatingService (+ implementations)  
- **http**: UserHandler, MediaHandler, RatingHandler  
- **util**: PasswordUtil (PBKDF2), TokenService (Bearer tokens)  

---

### **## API & Tests**

- Postman collection: `postman/MRP_Postman_Collection.json`
- curl demo: `scripts/demo.sh` (Linux/Mac) & `scripts/demo.cmd` (Windows)

---

### **## GitHub**

Repo: https://github.com/glenkishoti/mrp_project.git  

