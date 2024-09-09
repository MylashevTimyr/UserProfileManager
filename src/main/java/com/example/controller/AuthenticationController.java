package com.example.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import io.swagger.annotations.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Api(tags = "Управление аутентификацией", description = "Операции, связанные с аутентификацией и регистрацией пользователей")
public class AuthenticationController {

	private final AuthenticationService service;
	private final LogoutService logoutService;

	@PostMapping("/register")
	@ApiOperation(value = "Регистрация нового пользователя", notes = "Создает новую учетную запись пользователя с предоставленными данными")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь успешно зарегистрирован"),
			@ApiResponse(code = 400, message = "Некорректный запрос")
	})
	public ResponseEntity<AuthenticationResponse> register(
			@ApiParam(value = "Детали регистрации пользователя", required = true) @RequestBody RegisterRequest request
	) {
		return ResponseEntity.ok(service.register(request));
	}

	@PostMapping("/authenticate")
	@ApiOperation(value = "Аутентификация пользователя", notes = "Аутентифицирует пользователя и возвращает JWT токен")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь успешно аутентифицирован"),
			@ApiResponse(code = 401, message = "Некорректные учетные данные"),
			@ApiResponse(code = 400, message = "Некорректный запрос")
	})
	public ResponseEntity<AuthenticationResponse> authenticate(
			@ApiParam(value = "Детали аутентификации пользователя", required = true) @RequestBody AuthenticationRequest request
	) {
		return ResponseEntity.ok(service.authenticate(request));
	}

	@PostMapping("/logout")
	@ApiOperation(value = "Выход пользователя", notes = "Выходит из системы и аннулирует сеанс пользователя")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь успешно вышел из системы"),
			@ApiResponse(code = 400, message = "Некорректный запрос")
	})
	public ResponseEntity<Void> logout(
			@ApiParam(value = "HTTP запрос", required = true) HttpServletRequest request,
			@ApiParam(value = "HTTP ответ", required = true) HttpServletResponse response,
			@ApiParam(value = "Аутентификация пользователя", required = true) Authentication authentication
	) {
		logoutService.logout(request, response, authentication);
		return ResponseEntity.ok().build();
	}
}
