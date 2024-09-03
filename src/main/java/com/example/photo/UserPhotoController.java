package com.example.photo;

import com.example.user.User;
import com.example.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-photo")
@RequiredArgsConstructor
public class UserPhotoController {

	private final UserPhotoService service;
	private final UserService userService;

	// Сохранение фото для текущего аутентифицированного пользователя
	@PostMapping("/save")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> save(
			@RequestParam("photo") MultipartFile photo,
			Principal principal) {

		try {
			String username = principal.getName(); // Используем имя пользователя из токена
			service.save(username, photo); // Сохранение фото в базу данных
			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving photo: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	// Получение всех фото текущего пользователя или фото другого пользователя, если запрос делает администратор
	@GetMapping("/user-photos")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<List<UserPhotoResponseDto>> getUserPhotos(
			@RequestParam(required = false) Long userId,
			Principal principal) {

		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));

		List<UserPhoto> photos;

		if (currentUser.hasRole("ADMIN")) {
			if (userId != null) {
				photos = service.findAllByUserId(userId); // Администратор может запрашивать фото других пользователей
			} else {
				photos = service.findAllByUserId(currentUser.getId()); // Администратор может также видеть свои фото
			}
		} else {
			photos = service.findAllByUserId(currentUser.getId()); // Пользователь может видеть только свои фото
		}

		List<UserPhotoResponseDto> response = photos.stream()
				.map(UserPhotoResponseDto::new)
				.collect(Collectors.toList());

		return ResponseEntity.ok(response);
	}

	// Получение конкретного фото по его ID
	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> findById(@PathVariable Long id, Principal principal) {
		UserPhoto photo = service.findById(id);
		if (!service.isOwnerOrAdmin(principal.getName(), photo.getUser().getId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}

		try {
			Resource resource = service.loadPhoto(photo.getFilePath());
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	// Удаление фото
	@DeleteMapping("/{photoId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteById(
			@PathVariable Long photoId,
			@RequestParam(required = false) Long userId,
			Principal principal) {

		UserPhoto photo = service.findById(photoId);
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("User not found"));

		if (currentUser.hasRole("ADMIN")) {
			if (userId != null && !photo.getUser().getId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("The photo does not belong to the specified user.");
			}
		} else if (currentUser.hasRole("USER")) {
			if (!photo.getUser().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own photos.");
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			service.deleteById(photoId);  // This should only delete the photo, not the user
			return ResponseEntity.noContent().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting photo: " + e.getMessage());
		}
	}

	// Обновление фото
	@PutMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> updatePhoto(@PathVariable Long id, @RequestParam("photo") MultipartFile photo, Principal principal) {
		try {
			UserPhoto existingPhoto = service.findById(id);

			if (!service.isOwnerOrAdmin(principal.getName(), existingPhoto.getUser().getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own photos.");
			}

			service.updatePhoto(id, photo);
			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating photo: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
