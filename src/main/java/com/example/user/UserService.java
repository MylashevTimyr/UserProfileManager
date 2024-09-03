package com.example.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {

	private final PasswordEncoder passwordEncoder;
	private final UserRepository userRepository;

	public void changePassword(ChangePasswordRequest request, Principal principal) {
		// Получить аутентифицированного пользователя из Principal
		String currentPrincipalName = principal.getName();

		// Найти пользователя в вашей базе данных
		User user = userRepository.findByEmail(currentPrincipalName)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		// Проверить текущий пароль
		if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
			throw new IllegalArgumentException("Current password is incorrect");
		}

		// Установить новый пароль
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);
	}

	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new IllegalStateException("User not found"));
	}

	public UserResponseDto updateUser(Long id, User userDetails) {
		User user = getUserById(id); // Этот метод может выбросить исключение, если пользователь не найден

		// Проверка валидности предоставленных данных
		validateUserDetails(userDetails);

		// Обновление полей пользователя
		user.setFirstname(capitalize(userDetails.getFirstname()));
		user.setLastname(capitalize(userDetails.getLastname()));
		user.setMiddlename(userDetails.getMiddlename()); // Добавляем строку для обновления отчества
		user.setEmail(userDetails.getEmail());
		user.setPhoneNumber(userDetails.getPhoneNumber());

		// Добавляем строку для обновления даты рождения
		user.setBirthdate(userDetails.getBirthdate());

		try {
			// Сохранение обновленного пользователя
			User updatedUser = userRepository.save(user);

			// Преобразование в DTO
			return new UserResponseDto(updatedUser);
		} catch (Exception e) {
			throw new RuntimeException("Error updating user. Please try again later.");
		}
	}

	private void validateUserDetails(User userDetails) {
		// Проверка email
		if (userDetails.getEmail() == null || !userDetails.getEmail().contains("@")) {
			throw new IllegalArgumentException("Invalid email format.");
		}

		// Проверка даты (если предоставлена)
		if (userDetails.getBirthdate() != null && !isValidDate(userDetails.getBirthdate().toString())) {
			throw new IllegalArgumentException("Invalid date format. Expected format is yyyy-MM-dd.");
		}

		// Проверка ФИО
		if (userDetails.getFirstname() != null && !isCapitalized(userDetails.getFirstname())) {
			throw new IllegalArgumentException("Firstname must start with a capital letter.");
		}
		if (userDetails.getLastname() != null && !isCapitalized(userDetails.getLastname())) {
			throw new IllegalArgumentException("Lastname must start with a capital letter.");
		}

		// Проверка номера телефона
		if (userDetails.getPhoneNumber() != null && !isValidPhoneNumber(userDetails.getPhoneNumber())) {
			throw new IllegalArgumentException("Phone number must be in the format +7 *** *** ** **.");
		}
	}

	private boolean isValidLocalDate(LocalDate date) {
		// Проверяем, что дата не является нулевой
		return date != null;
	}

	private boolean isValidDate(String dateStr) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		try {
			LocalDate.parse(dateStr, formatter);
			return true;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	private boolean isCapitalized(String str) {
		return str.length() > 0 && Character.isUpperCase(str.charAt(0));
	}

	private boolean isValidPhoneNumber(String phoneNumber) {
		// Регулярное выражение для проверки формата номера телефона
		String phoneNumberPattern = "\\+7 \\d{3} \\d{3} \\d{2} \\d{2}";
		return Pattern.matches(phoneNumberPattern, phoneNumber);
	}

	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
	}

	public void deleteUser(Long id) {
		if (!userRepository.existsById(id)) {
			throw new IllegalStateException("User not found");
		}
		userRepository.deleteById(id);
	}

	public boolean checkPassword(User user, String rawPassword) {
		return passwordEncoder.matches(rawPassword, user.getPassword());
	}

	public Optional<User> findByUsername(String username) {
		return userRepository.findByEmail(username); // Предполагается, что username — это email
	}
}