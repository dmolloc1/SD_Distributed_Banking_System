package pe.unsa.sd.bankc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.bankc", "pe.unsa.sd.persistence"})
@EnableDiscoveryClient
public class BankCServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCServiceApplication.class, args);
    }
}
