//package com.alibou.security.photo;
//
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.UrlResource;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//
//@Service
//public class FileStorageService {
//
//	private final Path rootLocation = Paths.get("user-photos"); // Корневая папка для сохранения фото
//
//	public FileStorageService() throws IOException {
//		// Создаем корневую папку, если ее нет
//		Files.createDirectories(rootLocation);
//	}
//
//	public String saveFile(MultipartFile file, String userId) throws IOException {
//		// Проверяем и создаем корневую директорию
//		Files.createDirectories(rootLocation);
//
//		// Получаем оригинальное имя файла
//		String originalFilename = file.getOriginalFilename();
//
//		// Проверяем, что оригинальное имя файла не пустое
//		if (originalFilename == null || originalFilename.isEmpty()) {
//			throw new IllegalArgumentException("Файл не содержит имени.");
//		}
//
//		// Генерируем уникальное имя файла, чтобы избежать коллизий
//		String newFileName = userId + "_" + System.currentTimeMillis() + "_" + originalFilename;
//
//		// Получаем полный путь к файлу
//		Path filePath = rootLocation.resolve(newFileName).normalize();
//
//		// Сохраняем файл на диск
//		Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
//
//		// Возвращаем путь к сохраненному файлу
//		return filePath.toString();
//	}
//
//	public Resource loadFile(String filePath) throws MalformedURLException {
//		// Проверяем, что путь не пустой
//		if (filePath == null || filePath.isEmpty()) {
//			throw new IllegalArgumentException("Путь к файлу не может быть пустым.");
//		}
//
//		// Загружаем файл как ресурс
//		Path file = Paths.get(filePath).normalize();
//		Resource resource = new UrlResource(file.toUri());
//		if (resource.exists() && resource.isReadable()) {
//			return resource;
//		} else {
//			throw new RuntimeException("Не удалось прочитать файл: " + filePath);
//		}
//	}
//
//	public void deleteFile(String filePath) throws IOException {
//		// Проверяем, что путь не пустой
//		if (filePath == null || filePath.isEmpty()) {
//			throw new IllegalArgumentException("Путь к файлу не может быть пустым.");
//		}
//
//		// Удаляем файл, если он существует
//		Path file = Paths.get(filePath).normalize();
//		Files.deleteIfExists(file);
//	}
//
//	private String getFileExtension(String fileName) {
//		if (fileName == null || fileName.isEmpty()) {
//			throw new IllegalArgumentException("Имя файла не может быть пустым.");
//		}
//		int lastIndexOfDot = fileName.lastIndexOf('.');
//		return (lastIndexOfDot == -1) ? "" : fileName.substring(lastIndexOfDot);
//	}
//}
