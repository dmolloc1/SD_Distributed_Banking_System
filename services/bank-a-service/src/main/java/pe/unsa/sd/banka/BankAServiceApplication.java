package pe.unsa.sd.banka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.banka", "pe.unsa.sd.persistence"})
public class BankAServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankAServiceApplication.class, args);
    }
}
