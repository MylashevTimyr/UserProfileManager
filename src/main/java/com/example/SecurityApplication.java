package com.example;

import com.example.auth.AuthenticationService;
import com.example.auth.RegisterRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static com.example.user.Role.ADMIN;
import static com.example.user.Role.USER;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class SecurityApplication {

	public static void main(String[] args) {
		SpringApplication.run(SecurityApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(
			AuthenticationService service
	) {
		return args -> {
			// Создание пользователя с ролью USER
			var user = RegisterRequest.builder()
					.firstname("User")
					.lastname("User")
					.email("user@mail.com")
					.password("password")
					.role(USER)
					.build();
			System.out.println("User token: " + service.register(user).getAccessToken());

			var user2 = RegisterRequest.builder()
					.firstname("User2")
					.lastname("User2")
					.email("user2@mail.com")
					.password("password2")
					.role(USER)
					.build();
			System.out.println("User2 token: " + service.register(user2).getAccessToken());

			// Создание пользователя с ролью ADMIN
			var admin = RegisterRequest.builder()
					.firstname("Admin")
					.lastname("Admin")
					.email("admin@mail.com")
					.password("adminpassword")
					.role(ADMIN)
					.build();
			System.out.println("Admin token: " + service.register(admin).getAccessToken());
		};
	}
}
