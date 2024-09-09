package com.example.user;

import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Api(tags = "Управление пользователями", description = "Операции, связанные с управлением пользователями")
public class UserController {

	private final UserService userService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	@ApiOperation(value = "Получить всех пользователей", notes = "Получить всех пользователей с поддержкой пагинации")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователи успешно получены"),
			@ApiResponse(code = 403, message = "Доступ запрещен")
	})
	public ResponseEntity<Page<UserResponseDto>> getAllUsers(Pageable pageable) {
		Page<User> users = userService.getAllUsers(pageable);
		Page<UserResponseDto> response = users.map(UserResponseDto::new);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	@ApiOperation(value = "Получить пользователя по ID", notes = "Получить пользователя по его ID")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь успешно получен"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 404, message = "Пользователь не найден")
	})
	public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
		User user = userService.getUserById(id);
		UserResponseDto responseDto = new UserResponseDto(user);
		return ResponseEntity.ok(responseDto);
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	@ApiOperation(value = "Получить информацию о текущем пользователе", notes = "Получить информацию о текущем аутентифицированном пользователе")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Информация о текущем пользователе успешно получена"),
			@ApiResponse(code = 403, message = "Доступ запрещен")
	})
	public ResponseEntity<UserResponseDto> getCurrentUserInfo() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();
		User currentUser = userService.findByUsername(currentPrincipalName)
				.orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
		UserResponseDto responseDto = new UserResponseDto(currentUser);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/update")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Обновить текущего пользователя", notes = "Обновить данные текущего аутентифицированного пользователя")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пользователь успешно обновлен"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 404, message = "Пользователь не найден")
	})
	public ResponseEntity<UserResponseDto> updateCurrentUser(@RequestBody User userDetails, Principal principal) {
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

		UserResponseDto updatedUserDto = userService.updateUser(currentUser.getId(), userDetails);

		return ResponseEntity.ok(updatedUserDto);
	}

	@DeleteMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	@ApiOperation(value = "Удалить пользователя", notes = "Удалить пользователя по ID или текущего аутентифицированного пользователя")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = "Пользователь успешно удален"),
			@ApiResponse(code = 400, message = "Некорректный запрос"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 404, message = "Пользователь не найден")
	})
	public ResponseEntity<?> deleteUser(Principal principal, @RequestParam(required = false) Long id) {
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

		if (currentUser.hasRole("ADMIN")) {
			if (id != null && !currentUser.getId().equals(id)) {
				userService.deleteUser(id);
				return ResponseEntity.noContent().build();
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID требуется для удаления пользователя администратором.");
		}

		if (currentUser.hasRole("USER")) {
			userService.deleteUser(currentUser.getId());
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	@PatchMapping("/change-password")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Изменить пароль", notes = "Изменить пароль текущего аутентифицированного пользователя")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Пароль успешно изменен"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 400, message = "Некорректный запрос")
	})
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
		userService.changePassword(request, principal);
		return ResponseEntity.ok().build();
	}
}
