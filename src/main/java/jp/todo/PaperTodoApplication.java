package jp.todo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaperTodoApplication {
    public static void main(String[] args) { SpringApplication.run(PaperTodoApplication.class, args); }
}
