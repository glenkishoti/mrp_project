package at.fhtw.mrp;

import at.fhtw.mrp.http.MediaHandler;
import at.fhtw.mrp.http.RatingHandler;
import at.fhtw.mrp.http.UserHandler;
import at.fhtw.mrp.repo.*;
import at.fhtw.mrp.service.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {

        // ---- wiring: repositories ----
        IUserRepository   userRepo   = new UserRepository();
        IMediaRepository  mediaRepo  = new MediaRepository();
        IRatingRepository ratingRepo = new RatingRepository();

        // ---- wiring: services ----
        IAuthService   authService   = new AuthService(userRepo);
        IMediaService  mediaService  = new MediaService(mediaRepo);
        IRatingService ratingService = new RatingService(ratingRepo);

        // ---- wiring: HTTP handlers (thin) ----
        UserHandler   userHandler   = new UserHandler(authService, userRepo);
        MediaHandler  mediaHandler  = new MediaHandler(mediaService, ratingService, authService);
        RatingHandler ratingHandler = new RatingHandler(ratingService, authService, userRepo);

        // ---- HTTP server ----
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/users",   userHandler);
        server.createContext("/api/media",   mediaHandler);
        server.createContext("/api/ratings", ratingHandler);

        server.setExecutor(null); // default thread pool
        server.start();
        System.out.println("MRP server running at http://localhost:8080");
    }
}