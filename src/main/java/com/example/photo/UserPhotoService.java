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

	private final Path rootLocation = Paths.get("user-photos"); // Корневая папка для сохранения фото

	public void save(Long userId, MultipartFile photo) throws IOException {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

		// Validate the photo before saving
		validatePhotoFormat(photo);
		validatePhotoSize(photo);

		// Save the photo to the disk and get the file path
		String filePath = saveFile(photo, userId.toString());

		UserPhoto userPhoto = UserPhoto.builder()
				.user(user)
				.filePath(filePath)
				.build();

		repository.save(userPhoto);
	}

	public List<UserPhoto> findAllByUserId(Long userId) {
		return repository.findByUserId(userId);
	}

	public void deleteById(Long id) {
		UserPhoto photo = repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Photo not found with id: " + id));

		try {
			deleteFile(photo.getFilePath());
		} catch (IOException e) {
			throw new RuntimeException("Error deleting file: " + e.getMessage());
		}

		repository.deleteById(id);
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

		// Validate the new photo
		validatePhotoFormat(newPhoto);
		validatePhotoSize(newPhoto);

		// Save new photo to the disk
		String newFilePath = saveFile(newPhoto, existingPhoto.getUser().getId().toString());

		// Delete the old photo from the disk
		deleteFile(existingPhoto.getFilePath());

		// Update the photo path in the database
		existingPhoto.setFilePath(newFilePath);
		repository.save(existingPhoto);
	}

	private String saveFile(MultipartFile file, String userId) throws IOException {
		// Проверяем и создаем корневую директорию
		Files.createDirectories(rootLocation);

		// Получаем оригинальное имя файла
		String originalFilename = file.getOriginalFilename();

		// Проверяем, что оригинальное имя файла не пустое
		if (originalFilename == null || originalFilename.isEmpty()) {
			throw new IllegalArgumentException("Файл не содержит имени.");
		}

		// Генерируем уникальное имя файла, чтобы избежать коллизий
		String newFileName = userId + "_" + System.currentTimeMillis() + "_" + originalFilename;

		// Получаем полный путь к файлу
		Path filePath = rootLocation.resolve(newFileName).normalize();

		// Сохраняем файл на диск
		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

		// Возвращаем путь к сохраненному файлу
		return filePath.toString();
	}

	public Resource loadFile(String filePath) throws MalformedURLException {
		// Проверяем, что путь не пустой
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("Путь к файлу не может быть пустым.");
		}

		// Загружаем файл как ресурс
		Path file = Paths.get(filePath).normalize();
		Resource resource = new UrlResource(file.toUri());
		if (resource.exists() && resource.isReadable()) {
			return resource;
		} else {
			throw new RuntimeException("Не удалось прочитать файл: " + filePath);
		}
	}

	private void deleteFile(String filePath) throws IOException {
		// Проверяем, что путь не пустой
		if (filePath == null || filePath.isEmpty()) {
			throw new IllegalArgumentException("Путь к файлу не может быть пустым.");
		}

		// Удаляем файл, если он существует
		Path file = Paths.get(filePath).normalize();
		Files.deleteIfExists(file);
	}

	private void validatePhotoFormat(MultipartFile file) throws IOException {
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Файл не является изображением.");
		}
	}

	private void validatePhotoSize(MultipartFile file) {
		long maxSize = 5 * 1024 * 1024; // 5MB
		if (file.getSize() > maxSize) {
			throw new IllegalArgumentException("Размер файла превышает максимальный допустимый размер 5MB.");
		}
	}

	private String getFileExtension(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			throw new IllegalArgumentException("Имя файла не может быть пустым.");
		}
		int lastIndexOfDot = fileName.lastIndexOf('.');
		return (lastIndexOfDot == -1) ? "" : fileName.substring(lastIndexOfDot);
	}
}
