package com.example.user;

import com.example.photo.UserPhotoResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

	private Long id;
	private String firstname;
	private String lastname;
	private String middlename;
	private String birthdate;
	private Integer age;
	private String phoneNumber;
	private String email;
	private List<UserPhotoResponseDto> photos; // Список фотографий

	public UserResponseDto(User user) {
		this.id = user.getId();
		this.firstname = user.getFirstname();
		this.lastname = user.getLastname();
		this.middlename = user.getMiddlename();
		this.birthdate = user.getBirthdate() != null ? user.getBirthdate().toString() : null;
		this.age = calculateAge(user.getBirthdate());
		this.phoneNumber = user.getPhoneNumber();
		this.email = user.getEmail();
		this.photos = user.getPhotos() != null ?
				user.getPhotos().stream()
						.map(UserPhotoResponseDto::new)
						.toList() : null;
	}

	private Integer calculateAge(LocalDate birthdate) {
		if (birthdate == null) {
			return null;
		}
		return Period.between(birthdate, LocalDate.now()).getYears();
	}
}
