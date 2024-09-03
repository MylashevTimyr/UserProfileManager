package com.example.photo;

import com.example.user.User;
import com.example.user.UserService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.Principal;

@RestController
@RequestMapping("/api/user-photo")
@RequiredArgsConstructor
@Api(tags = "Управление фотографиями пользователей", description = "Операции, связанные с управлением фотографиями пользователей")
public class UserPhotoController {

	private final UserPhotoService service;
	private final UserService userService;

	@PostMapping("/save")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Сохранить фотографию", notes = "Загрузить и сохранить новую фотографию для аутентифицированного пользователя")
	@ApiResponses(value = {
			@ApiResponse(code = 202, message = "Фотография успешно сохранена"),
			@ApiResponse(code = 500, message = "Ошибка при сохранении фотографии"),
			@ApiResponse(code = 400, message = "Некорректный запрос")
	})
	public ResponseEntity<?> save(
			@ApiParam(value = "Фотография для загрузки", required = true) @RequestParam("photo") MultipartFile photo,
			Principal principal) {

		try {
			String username = principal.getName();
			service.save(username, photo);
			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при сохранении фотографии: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/user-photos")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	@ApiOperation(value = "Получить фотографии пользователя", notes = "Получить фотографии аутентифицированного пользователя или указанного пользователя (если администратор)")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Фотографии успешно получены"),
			@ApiResponse(code = 403, message = "Доступ запрещен")
	})
	public ResponseEntity<Page<UserPhotoResponseDto>> getUserPhotos(
			@ApiParam(value = "ID пользователя для получения фотографий (только для администратора)", required = false) @RequestParam(required = false) Long userId,
			Principal principal,
			Pageable pageable) {

		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

		Page<UserPhoto> photos;

		if (currentUser.hasRole("ADMIN")) {
			if (userId != null) {
				photos = service.findAllByUserId(userId, pageable);
			} else {
				photos = service.findAllByUserId(currentUser.getId(), pageable);
			}
		} else {
			photos = service.findAllByUserId(currentUser.getId(), pageable);
		}

		Page<UserPhotoResponseDto> response = photos.map(UserPhotoResponseDto::new);

		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Получить фотографию по ID", notes = "Получить фотографию по её ID, если пользователь является её владельцем или является администратором")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Фотография успешно получена"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 500, message = "Ошибка при загрузке фотографии")
	})
	public ResponseEntity<Resource> findById(
			@ApiParam(value = "ID фотографии для получения", required = true) @PathVariable Long id,
			Principal principal) {
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

	@DeleteMapping("/{photoId}")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Удалить фотографию", notes = "Удалить фотографию по её ID, если пользователь является её владельцем или является администратором")
	@ApiResponses(value = {
			@ApiResponse(code = 204, message = "Фотография успешно удалена"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 400, message = "Некорректный запрос"),
			@ApiResponse(code = 500, message = "Ошибка при удалении фотографии")
	})
	public ResponseEntity<?> deleteById(
			@ApiParam(value = "ID фотографии для удаления", required = true) @PathVariable Long photoId,
			@ApiParam(value = "ID пользователя (только для администратора)", required = false) @RequestParam(required = false) Long userId,
			Principal principal) {

		UserPhoto photo = service.findById(photoId);
		User currentUser = userService.findByUsername(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Пользователь не найден"));

		if (currentUser.hasRole("ADMIN")) {
			if (userId != null && !photo.getUser().getId().equals(userId)) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Фотография не принадлежит указанному пользователю.");
			}
		} else if (currentUser.hasRole("USER")) {
			if (!photo.getUser().getId().equals(currentUser.getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы можете удалить только свои собственные фотографии.");
			}
		} else {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			service.deleteById(photoId);
			return ResponseEntity.noContent().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при удалении фотографии: " + e.getMessage());
		}
	}

	@PutMapping("/{id}")
	@PreAuthorize("isAuthenticated()")
	@ApiOperation(value = "Обновить фотографию", notes = "Обновить фотографию по её ID, если пользователь является её владельцем или является администратором")
	@ApiResponses(value = {
			@ApiResponse(code = 202, message = "Фотография успешно обновлена"),
			@ApiResponse(code = 403, message = "Доступ запрещен"),
			@ApiResponse(code = 400, message = "Некорректный запрос"),
			@ApiResponse(code = 500, message = "Ошибка при обновлении фотографии")
	})
	public ResponseEntity<?> updatePhoto(
			@ApiParam(value = "ID фотографии для обновления", required = true) @PathVariable Long id,
			@ApiParam(value = "Новый файл фотографии", required = true) @RequestParam("photo") MultipartFile photo,
			Principal principal) {
		try {
			UserPhoto existingPhoto = service.findById(id);

			if (!service.isOwnerOrAdmin(principal.getName(), existingPhoto.getUser().getId())) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Вы можете обновить только свои собственные фотографии.");
			}

			service.updatePhoto(id, photo);
			return ResponseEntity.accepted().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при обновлении фотографии: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}
