package com.devops.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class App {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(PORT), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/health", new HealthHandler());

        server.setExecutor(null);

        System.out.println("Application started on port " + PORT);

        server.start();
    }

    public static String getGreeting() {
        return "Automated CI/CD Pipeline on AWS - Deployment Successful!";
    }

    static class RootHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String response = "<h1>" + getGreeting()
                    + "</h1><p>Status: Healthy</p>";

            byte[] responseBytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "text/html; charset=UTF-8");

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }

    static class HealthHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String response = "{\"status\":\"UP\"}";

            byte[] responseBytes =
                    response.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json");

            exchange.sendResponseHeaders(200, responseBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}