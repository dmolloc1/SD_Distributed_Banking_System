package pe.unsa.sd.bankb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"pe.unsa.sd.bankb", "pe.unsa.sd.persistence"})
public class BankBServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankBServiceApplication.class, args);
    }
}
