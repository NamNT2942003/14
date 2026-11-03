package com.quickbite.user;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class UserServiceApplication {
    // Bộ đếm metrics cho các request
    private static final AtomicLong helloRequests = new AtomicLong(0);
    private static final AtomicLong errorRequests = new AtomicLong(0);

    public static void main(String[] args) throws IOException {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        System.out.println("  .   ____          _            __ _ _");
        System.out.println(" /\\\\ / ___'_ __ _ _(_)_ __  __ _ \\ \\ \\ \\");
        System.out.println("( ( )\\___ | '_ | '_| | '_ \\/ _` | \\ \\ \\ \\");
        System.out.println(" \\\\/  ___)| |_)| | | | | || (_| |  ) ) ) )");
        System.out.println("  '  |____| .__|_| |_|_| |_\\__, | / / / /");
        System.out.println(" =========|_|==============|___/=/_/_/_/");
        System.out.println(" :: Spring Boot ::                (v3.1.2)");
        System.out.println();
        System.out.println(now + "  INFO 1 --- [main] c.q.u.UserServiceApplication          : Starting QuickBite UserService with Micrometer & Prometheus...");

        int port = 8081;
        String portEnv = System.getenv("SERVER_PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            try { port = Integer.parseInt(portEnv); } catch (Exception ignored) {}
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);

        // 1. Endpoint /api/hello (Thành công HTTP 200)
        server.createContext("/api/hello", exchange -> {
            helloRequests.incrementAndGet();
            String response = "{\"message\":\"Hello from QuickBite Backend NOC!\",\"status\":\"SUCCESS\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";
            sendJsonResponse(exchange, 200, response);
        });

        // 2. Endpoint /api/error (Cố tình tạo lỗi HTTP 500 để kiểm thử cảnh báo Alerting)
        server.createContext("/api/error", exchange -> {
            errorRequests.incrementAndGet();
            String response = "{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Simulated Exception for NOC Alert Testing!\"}";
            sendJsonResponse(exchange, 500, response);
        });

        // 3. Endpoint /actuator/health
        server.createContext("/actuator/health", exchange -> {
            String response = "{\"status\":\"UP\",\"service\":\"quickbite-user-service\",\"port\":" + port + "}";
            sendJsonResponse(exchange, 200, response);
        });

        // Endpoint mặc định
        server.createContext("/", exchange -> {
            String response = "{\"service\":\"quickbite-user-service\",\"status\":\"RUNNING\"}";
            sendJsonResponse(exchange, 200, response);
        });

        // 4. Endpoint /actuator/prometheus (Chuẩn định dạng Prometheus text format)
        server.createContext("/actuator/prometheus", exchange -> {
            Runtime rt = Runtime.getRuntime();
            long usedHeap = rt.totalMemory() - rt.freeMemory();
            long maxHeap = rt.maxMemory();
            long committedHeap = rt.totalMemory();
            int liveThreads = Thread.activeCount();

            StringBuilder sb = new StringBuilder();
            sb.append("# HELP http_server_requests_seconds_count Number of HTTP requests processed\n");
            sb.append("# TYPE http_server_requests_seconds_count counter\n");
            sb.append("http_server_requests_seconds_count{application=\"quickbite-user-service\",uri=\"/api/hello\",status=\"200\",method=\"GET\"} ").append(helloRequests.get()).append("\n");
            sb.append("http_server_requests_seconds_count{application=\"quickbite-user-service\",uri=\"/api/error\",status=\"500\",method=\"GET\"} ").append(errorRequests.get()).append("\n\n");

            sb.append("# HELP jvm_memory_used_bytes The amount of used memory\n");
            sb.append("# TYPE jvm_memory_used_bytes gauge\n");
            sb.append("jvm_memory_used_bytes{application=\"quickbite-user-service\",area=\"heap\",id=\"G1 Survivor Space\"} ").append(usedHeap).append("\n");
            sb.append("jvm_memory_committed_bytes{application=\"quickbite-user-service\",area=\"heap\"} ").append(committedHeap).append("\n");
            sb.append("jvm_memory_max_bytes{application=\"quickbite-user-service\",area=\"heap\"} ").append(maxHeap).append("\n\n");

            sb.append("# HELP jvm_threads_live_threads The current number of live threads\n");
            sb.append("# TYPE jvm_threads_live_threads gauge\n");
            sb.append("jvm_threads_live_threads{application=\"quickbite-user-service\"} ").append(liveThreads).append("\n");

            byte[] bytes = sb.toString().getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();

        System.out.println(now + "  INFO 1 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port: " + port);
        System.out.println(now + "  INFO 1 --- [main] c.q.u.UserServiceApplication          : Started UserServiceApplication successfully!");
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
