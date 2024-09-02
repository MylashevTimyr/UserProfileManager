package com.example.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	// Получить список всех пользователей (доступно только для ADMIN)
	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		List<User> users = userService.getAllUsers();
		List<UserResponseDto> response = users.stream().map(UserResponseDto::new).collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	// Получить данные конкретного пользователя по ID (доступно только для ADMIN)
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
		User user = userService.getUserById(id);
		UserResponseDto responseDto = new UserResponseDto(user);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long id, @RequestBody User userDetails, Principal principal) {
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));
		if (!currentUser.getId().equals(id) && !currentUser.hasRole("ADMIN")) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		UserResponseDto updatedUserDto = userService.updateUser(id, userDetails);
		return ResponseEntity.ok(updatedUserDto);
	}

	// Удалить пользователя (ADMIN по ID, USER может удалить только свою учетную запись)
	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	public ResponseEntity<?> deleteUser(@PathVariable Long id, Principal principal, @RequestParam(required = false) String password) {
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));

		if (currentUser.hasRole("ADMIN")) {
			userService.deleteUser(id);
			return ResponseEntity.noContent().build();
		}

		if (currentUser.getId().equals(id)) {
			if (password == null || !userService.checkPassword(currentUser, password)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid password.");
			}
			userService.deleteUser(id);
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	// Изменить пароль пользователя (доступно только авторизованным пользователям)
	@PatchMapping("/change-password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
		userService.changePassword(request, principal);
		return ResponseEntity.ok().build();
	}
}