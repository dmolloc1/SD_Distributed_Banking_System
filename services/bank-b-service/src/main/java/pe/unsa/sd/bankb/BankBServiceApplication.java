package pe.unsa.sd.bankb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.bankb", "pe.unsa.sd.persistence"})
@EnableDiscoveryClient
public class BankBServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankBServiceApplication.class, args);
    }
}
