package com.example.photo;

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

	@PostMapping("/save")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> save(
			@RequestParam("userId") Long userId,
			@RequestParam("photo") MultipartFile photo,
			Principal principal) {

		try {
			if (!service.isOwnerOrAdmin(principal.getName(), userId)) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only add photos to your account.");
			}

			// Сохранение фото на диск и в базу данных
			service.save(userId, photo);
			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving photo: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<UserPhotoResponseDto>> findAllUserPhoto() {
		List<UserPhoto> photos = service.findAll();
		List<UserPhotoResponseDto> response = photos.stream()
				.map(UserPhotoResponseDto::new)
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/user/{userId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<UserPhotoResponseDto>> findAllByUserId(@PathVariable Long userId, Principal principal) {
		if (!service.isOwnerOrAdmin(principal.getName(), userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}
		List<UserPhoto> photos = service.findAllByUserId(userId);
		List<UserPhotoResponseDto> response = photos.stream()
				.map(UserPhotoResponseDto::new)
				.collect(Collectors.toList());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Resource> findById(@PathVariable Long id, Principal principal) {
		UserPhoto photo = service.findById(id);
		if (!service.isOwnerOrAdmin(principal.getName(), photo.getUser().getId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
		}

		try {
			Resource file = service.loadFile(photo.getFilePath());
			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
					.body(file);
		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> deleteById(@PathVariable Long id, Principal principal) {
		UserPhoto photo = service.findById(id);
		if (!service.isOwnerOrAdmin(principal.getName(), photo.getUser().getId())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your photos.");
		}

		// Удаление фото с диска и из базы данных
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<?> updatePhoto(@PathVariable Long id, @RequestParam("photo") MultipartFile photo, Principal principal) {
		try {
			UserPhoto existingPhoto = service.findById(id);

			if (!service.isOwnerOrAdmin(principal.getName(), existingPhoto.getUser().getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only update your own photos.");
			}

			// Обновление фото на диске и в базе данных
			service.updatePhoto(id, photo);

			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating photo: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
