package com.example.photo;

import com.example.user.User;
import com.example.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

		String photoPath = savePhoto(photo, user.getId().toString());

		UserPhoto userPhoto = UserPhoto.builder()
				.user(user)
				.filePath(photoPath)
				.build();

		repository.save(userPhoto);
	}

	public List<UserPhoto> findAllByUserId(Long userId) {
		return repository.findByUserId(userId);
	}

	public void deleteById(Long photoId) throws IOException {
		UserPhoto photo = repository.findById(photoId)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + photoId));

		// Delete photo file from disk
		deletePhoto(photo.getFilePath());

		// Delete photo record from the database
		repository.delete(photo);
	}

	public UserPhoto findById(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + id));
	}

	public List<UserPhoto> findAll() {
		return repository.findAll();
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

	private String savePhoto(MultipartFile photo, String userId) throws IOException {
		Files.createDirectories(rootLocation);

		String originalFilename = photo.getOriginalFilename();

		if (originalFilename == null || originalFilename.isEmpty()) {
			throw new IllegalArgumentException("Фотография не содержит имени.");
		}

		String newPhotoName = userId + "_" + System.currentTimeMillis() + "_" + originalFilename;

		Path photoPath = rootLocation.resolve(newPhotoName).normalize();

		Files.copy(photo.getInputStream(), photoPath, StandardCopyOption.REPLACE_EXISTING);

		return photoPath.toString();
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
		long maxSize = 5 * 1024 * 1024; // 5MB
		if (photo.getSize() > maxSize) {
			throw new IllegalArgumentException("Размер фотографии превышает максимальный допустимый размер 5MB.");
		}
	}
}
