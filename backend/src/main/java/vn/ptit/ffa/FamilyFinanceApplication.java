package vn.ptit.ffa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class FamilyFinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyFinanceApplication.class, args);
    }
}
