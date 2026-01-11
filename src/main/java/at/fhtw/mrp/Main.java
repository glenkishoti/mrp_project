package at.fhtw.mrp;

import at.fhtw.mrp.http.UserHandler;
import at.fhtw.mrp.http.MediaHandler;
import at.fhtw.mrp.http.RatingHandler;
import at.fhtw.mrp.http.FavoriteHandler;
import at.fhtw.mrp.repo.*;
import at.fhtw.mrp.service.*;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {

        // LAYER 1: Create repositories
        UserRepository   userRepo     = new UserRepository();
        MediaRepository  mediaRepo    = new MediaRepository();
        RatingRepository ratingRepo   = new RatingRepository();
        FavoriteRepository favoriteRepo = new FavoriteRepository();

        // LAYER 2: Create services
        AuthService     authService     = new AuthService(userRepo);
        MediaService    mediaService    = new MediaService(mediaRepo);
        RatingService   ratingService   = new RatingService(ratingRepo);
        FavoriteService favoriteService = new FavoriteService(favoriteRepo, mediaRepo);

        // LAYER 3: Create HTTP handlers
        UserHandler     userHandler     = new UserHandler(authService, userRepo);
        MediaHandler    mediaHandler    = new MediaHandler(mediaService, ratingService, authService);
        RatingHandler   ratingHandler   = new RatingHandler(ratingService, authService, userRepo);
        FavoriteHandler favoriteHandler = new FavoriteHandler(favoriteService, userRepo);

        // LAYER 4: Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Register handlers for URL paths
        server.createContext("/api/users",     userHandler);     // All /api/users/* requests
        server.createContext("/api/media",     mediaHandler);    // All /api/media/* requests
        server.createContext("/api/ratings",   ratingHandler);   // All /api/ratings/* requests
        server.createContext("/api/favorites", favoriteHandler); // All /api/favorites/* requests

        server.setExecutor(null);
        server.start();

        System.out.println("MRP server running at http://localhost:8080");
        System.out.println("Available endpoints:");
        System.out.println("  - /api/users/*");
        System.out.println("  - /api/media/*");
        System.out.println("  - /api/ratings/*");
        System.out.println("  - /api/favorites/*");
    }
}