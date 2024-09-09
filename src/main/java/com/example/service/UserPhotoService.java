package com.example.photo;

import com.example.user.User;
import com.example.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class UserPhotoService {

	private final UserPhotoRepository repository;
	private final UserRepository userRepository;

	private final Path rootLocation = Paths.get("user-photos");

	public void save(String username, MultipartFile photo) throws IOException {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found with email: " + username));

		validatePhotoFormat(photo);
		validatePhotoSize(photo);

		// Проверка, существует ли уже такая фотография у пользователя
		boolean photoExists = repository.existsByUserAndFileName(user, photo.getOriginalFilename());
		if (photoExists) {
			throw new IllegalArgumentException("This photo already exists for the user.");
		}

		// Сохранение фотографии с оригинальным именем файла
		String photoPath = savePhoto(photo, photo.getOriginalFilename());

		UserPhoto userPhoto = UserPhoto.builder()
				.user(user)
				.filePath(photoPath)
				.fileName(photo.getOriginalFilename())  // сохраняем оригинальное имя файла для отображения
				.build();

		repository.save(userPhoto);
	}

	public Page<UserPhoto> findAllByUserId(Long userId, Pageable pageable) {
		return repository.findByUserId(userId, pageable);
	}

	public Resource loadPhoto(String photoPath) throws MalformedURLException {
		if (photoPath == null || photoPath.isEmpty()) {
			throw new IllegalArgumentException("Путь к фотографии не может быть пустым.");
		}

		Path photo = Paths.get(photoPath).normalize();
		Resource resource = new UrlResource(photo.toUri());
		if (resource.exists() && resource.isReadable()) {
			return resource;
		} else {
			throw new RuntimeException("Не удалось прочитать фотографию: " + photoPath);
		}
	}

	public void deleteById(Long photoId) throws IOException {
		UserPhoto photo = repository.findById(photoId)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + photoId));

		deletePhoto(photo.getFilePath());

		repository.delete(photo);
	}

	public UserPhoto findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + id));
	}

	public boolean isOwnerOrAdmin(String username, Long userId) {
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found with email: " + username));
		return user.getId().equals(userId) || user.getRole().name().equals("ADMIN");
	}

	public void updatePhoto(Long id, MultipartFile newPhoto) throws IOException {
		UserPhoto existingPhoto = repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + id));

		validatePhotoFormat(newPhoto);
		validatePhotoSize(newPhoto);

		String newPhotoPath = savePhoto(newPhoto, existingPhoto.getUser().getId().toString());

		deletePhoto(existingPhoto.getFilePath());

		existingPhoto.setFilePath(newPhotoPath);
		repository.save(existingPhoto);
	}

	private String savePhoto(MultipartFile photo, String originalFileName) throws IOException {
		Path destinationFile = this.rootLocation.resolve(
						Paths.get(originalFileName))
				.normalize().toAbsolutePath();

		try (InputStream inputStream = photo.getInputStream()) {
			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
		}

		return destinationFile.toString();
	}

	private void deletePhoto(String photoPath) throws IOException {
		if (photoPath == null || photoPath.isEmpty()) {
			throw new IllegalArgumentException("Путь к фотографии не может быть пустым.");
		}

		Path photo = Paths.get(photoPath).normalize();
		Files.deleteIfExists(photo);
	}

	private void validatePhotoFormat(MultipartFile photo) throws IOException {
		String contentType = photo.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Файл не является изображением.");
		}
	}

	private void validatePhotoSize(MultipartFile photo) {
		long maxSize = 5 * 1024 * 1024;
		if (photo.getSize() > maxSize) {
			throw new IllegalArgumentException("Размер фотографии превышает максимальный допустимый размер 5MB.");
		}
	}
}
