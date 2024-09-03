//package com.example.user;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.test.web.server.LocalServerPort;
//import org.springframework.http.MediaType;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//public class UserControllerTest {
//
//	@Autowired
//	private MockMvc mockMvc;
//
//	@Autowired
//	private UserRepository userRepository;
//
//	@Autowired
//	private PasswordEncoder passwordEncoder;
//
//	@Autowired
//	private TestRestTemplate testRestTemplate;
//
//	@LocalServerPort
//	private int localServerPort;
//
//	@BeforeEach
//	public void setup() {
//		// Очистка базы данных перед каждым тестом
//		userRepository.deleteAll();
//
//		// Создание тестового пользователя с ролью ADMIN
//		User adminUser = new User();
//		adminUser.setEmail("admin@example.com");
//		adminUser.setPassword(passwordEncoder.encode("password")); // Шифрование пароля
//		adminUser.setRole(Role.ADMIN);
//		userRepository.save(adminUser);
//
//		// Создание тестового пользователя с ролью USER
//		User normalUser = new User();
//		normalUser.setEmail("user@example.com");
//		normalUser.setPassword(passwordEncoder.encode("password")); // Шифрование пароля
//		normalUser.setRole(Role.USER);
//		userRepository.save(normalUser);
//	}
//
//	@Test
//	public void getAllUsers() throws Exception {
//		mockMvc.perform(get("/api/user")
//						.with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$").isArray())
//				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
//				.andExpect(jsonPath("$[1].email").value("user@example.com"))
//				.andDo(print());
//	}
//
//	@Test
//	public void getUserById() throws Exception {
//		// Получаем ID пользователя
//		Long userId = userRepository.findByEmail("user@example.com").get().getId();
//
//		mockMvc.perform(get("/api/user/{id}", userId)
//						.with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$.email").value("user@example.com"))
//				.andDo(print());
//	}
//
//	@Test
//	public void updateUser() throws Exception {
//		// Получаем ID пользователя
//		Long userId = userRepository.findByEmail("user@example.com").get().getId();
//
//		// Подготовка JSON тела запроса для обновления пользователя
//		String requestBody = """
//                {
//                    "firstname": "John",
//                    "lastname": "Doe",
//                    "email": "john.doe@example.com",
//                    "phoneNumber": "+7 123 456 78 90",
//                    "birthdate": "1990-01-01"
//                }""";
//
//		mockMvc.perform(put("/api/user/{id}", userId)
//						.with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN"))
//						.contentType(MediaType.APPLICATION_JSON)
//						.content(requestBody))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$.email").value("john.doe@example.com"))
//				.andDo(print());
//	}
//
//	@Test
//	public void deleteUser() throws Exception {
//		// Получаем ID пользователя
//		Long userId = userRepository.findByEmail("user@example.com").get().getId();
//
//		mockMvc.perform(delete("/api/user/{id}", userId)
//						.param("password", "password")  // Используйте правильный пароль
//						.with(SecurityMockMvcRequestPostProcessors.user("admin@example.com").roles("ADMIN")))
//				.andExpect(status().isNoContent())
//				.andDo(print());
//	}
//
//	@Test
//	public void changePassword() throws Exception {
//		// Измените password в соответствии с текущим зашифрованным паролем пользователя в базе данных
//		String request = """
//        {
//            "currentPassword": "password",
//            "newPassword": "newpassword",
//            "confirmationPassword": "newpassword"
//        }""";
//
//		mockMvc.perform(patch("/api/user/change-password")
//						.with(SecurityMockMvcRequestPostProcessors.user("user@example.com").roles("USER"))
//						.contentType(MediaType.APPLICATION_JSON)
//						.content(request))
//				.andExpect(status().isOk())
//				.andDo(print());
//	}
//}
