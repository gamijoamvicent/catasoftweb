package devforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"devforge", "model"})
public class InventsoftApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventsoftApplication.class, args);
    }
}