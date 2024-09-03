//package com.example.user;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//public class AuthenticationControllerTest {
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
//	@BeforeEach
//	public void setup() {
//		// Очистка базы данных перед каждым тестом
//		userRepository.deleteAll();
//
//		// Создание тестового пользователя с ролью USER
//		User user = new User();
//		user.setEmail("user@example.com");
//		user.setPassword(passwordEncoder.encode("password")); // Шифрование пароля
//		user.setRole(Role.USER);
//		userRepository.save(user);
//
//		// Создание тестового пользователя с ролью ADMIN
//		User adminUser = new User();
//		adminUser.setEmail("admin@example.com");
//		adminUser.setPassword(passwordEncoder.encode("password")); // Шифрование пароля
//		adminUser.setRole(Role.ADMIN);
//		userRepository.save(adminUser);
//	}
//
//	@Test
//	public void testRegisterUser() throws Exception {
//		// Тело запроса для регистрации нового пользователя
//		String requestBody = """
//            {
//                "firstname": "Test",
//                "lastname": "User",
//                "email": "newuser@example.com",
//                "password": "newpassword",
//                "role": "USER"
//            }""";
//
//		mockMvc.perform(post("/api/users/register")
//						.contentType(MediaType.APPLICATION_JSON)
//						.content(requestBody))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$.access_token").isNotEmpty())  // Проверка, что токен не пуст
//				.andDo(print());
//	}
//
//	@Test
//	public void testAuthenticateUser() throws Exception {
//		// Тело запроса для аутентификации существующего пользователя
//		String requestBody = """
//            {
//                "email": "user@example.com",
//                "password": "password"
//            }""";
//
//		mockMvc.perform(post("/api/users/authenticate")
//						.contentType(MediaType.APPLICATION_JSON)
//						.content(requestBody))
//				.andExpect(status().isOk())
//				.andExpect(jsonPath("$.access_token").isNotEmpty())  // Проверка, что токен не пуст
//				.andDo(print());
//	}
//
//	@Test
//	public void testLogoutUser() throws Exception {
//		// Сначала аутентифицируемся, чтобы получить валидный JWT токен
//		String authRequest = """
//            {
//                "email": "user@example.com",
//                "password": "password"
//            }""";
//
//		String token = mockMvc.perform(post("/api/users/authenticate")
//						.contentType(MediaType.APPLICATION_JSON)
//						.content(authRequest))
//				.andExpect(status().isOk())
//				.andReturn()
//				.getResponse()
//				.getContentAsString()
//				.split(":")[1]
//				.replaceAll("[\"}]", "").trim();
//
//		// Выходим из системы, используя полученный токен
//		mockMvc.perform(post("/api/users/logout")
//						.header("Authorization", "Bearer " + token))
//				.andExpect(status().isOk())
//				.andDo(print());
//	}
//}
