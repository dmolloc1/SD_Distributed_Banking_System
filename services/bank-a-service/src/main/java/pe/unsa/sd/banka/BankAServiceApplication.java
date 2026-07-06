package pe.unsa.sd.banka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.banka", "pe.unsa.sd.persistence"})
@EnableDiscoveryClient
public class BankAServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankAServiceApplication.class, args);
    }
}
