package pe.unsa.sd.bankc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.banka", "pe.unsa.sd.persistence"})
public class BankCServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCServiceApplication.class, args);
    }
}
