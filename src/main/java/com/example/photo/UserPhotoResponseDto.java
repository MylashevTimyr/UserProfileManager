package com.example.photo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPhotoResponseDto {

	private Long id;
	private String fileName; // Имя файла
	private LocalDateTime createDate;
	private String fileSize; // Размер файла с указанием единиц измерения

	public UserPhotoResponseDto(UserPhoto photo) {
		this.id = photo.getId();
		this.fileName = extractFileName(photo.getFilePath());
		this.createDate = photo.getCreateDate();
		this.fileSize = calculateAndFormatFileSize(photo.getFilePath());
	}

	private String extractFileName(String filePath) {
		// Извлекаем только имя файла из полного пути
		return filePath != null ? Paths.get(filePath).getFileName().toString() : null;
	}

	private String calculateAndFormatFileSize(String filePath) {
		if (filePath == null) return "Unknown size";
		try {
			Path path = Paths.get(filePath);
			long sizeInBytes = Files.size(path);
			return formatFileSize(sizeInBytes);
		} catch (IOException e) {
			return "Could not determine size";
		}
	}

	private String formatFileSize(long sizeInBytes) {
		if (sizeInBytes < 1024) {
			return sizeInBytes + " B";
		} else if (sizeInBytes < 1024 * 1024) {
			return (sizeInBytes / 1024) + " KB";
		} else {
			return (sizeInBytes / (1024 * 1024)) + " MB";
		}
	}
}
