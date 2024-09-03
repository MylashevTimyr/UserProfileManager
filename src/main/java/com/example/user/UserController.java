package com.example.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserResponseDto>> getAllUsers() {
		List<User> users = userService.getAllUsers();
		List<UserResponseDto> response = users.stream().map(UserResponseDto::new).collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
		User user = userService.getUserById(id);
		UserResponseDto responseDto = new UserResponseDto(user);
		return ResponseEntity.ok(responseDto);
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<UserResponseDto> getCurrentUserInfo() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String currentPrincipalName = authentication.getName();
		User currentUser = userService.findByUsername(currentPrincipalName)
				.orElseThrow(() -> new IllegalStateException("User not found"));
		UserResponseDto responseDto = new UserResponseDto(currentUser);
		return ResponseEntity.ok(responseDto);
	}

	@PutMapping("/update")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<UserResponseDto> updateCurrentUser(@RequestBody User userDetails, Principal principal) {
		// Получаем текущего пользователя из токена
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));

		// Обновляем данные текущего пользователя
		UserResponseDto updatedUserDto = userService.updateUser(currentUser.getId(), userDetails);

		return ResponseEntity.ok(updatedUserDto);
	}

	@DeleteMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	public ResponseEntity<?> deleteUser(Principal principal, @RequestParam(required = false) Long id) {
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));

		if (currentUser.hasRole("ADMIN")) {
			// Если администратор, он может удалить пользователя по ID
			if (id != null && !currentUser.getId().equals(id)) {
				userService.deleteUser(id);
				return ResponseEntity.noContent().build();
			}
			// Если ID не предоставлен, администратор не может удалить без ID
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ID is required for admin to delete user.");
		}

		// Если пользователь, он может удалить только себя
		if (currentUser.hasRole("USER")) {
			userService.deleteUser(currentUser.getId());
			return ResponseEntity.noContent().build();
		}

		return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
	}

	@PatchMapping("/change-password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
		userService.changePassword(request, principal);
		return ResponseEntity.ok().build();
	}
}