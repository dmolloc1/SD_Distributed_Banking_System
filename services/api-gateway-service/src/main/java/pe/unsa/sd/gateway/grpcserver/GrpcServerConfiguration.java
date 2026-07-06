package pe.unsa.sd.gateway.grpcserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerConfiguration.class);

    private final int port;
    private final List<BindableService> services;
    private Server server;

    public GrpcServerConfiguration(
            @Value("${gateway.grpc.port}") int port,
            List<BindableService> services) {
        this.port = port;
        this.services = services;
    }

    @PostConstruct
    public void start() throws IOException {
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port);
        services.forEach(serverBuilder::addService);
        server = serverBuilder.build().start();
        log.info("API Gateway gRPC server started on port {}", port);
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.shutdown();
            log.info("API Gateway gRPC server stopped");
        }
    }
}
