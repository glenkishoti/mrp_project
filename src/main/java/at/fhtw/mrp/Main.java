package at.fhtw.mrp;

import at.fhtw.mrp.http.UserHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/users", new UserHandler());
        //The server will create/manage worker threads itself from its built-in thread pool;
        server.setExecutor(null); // default executor
        server.start();
        System.out.println("MRP server running at http://localhost:8080");
    }
}
