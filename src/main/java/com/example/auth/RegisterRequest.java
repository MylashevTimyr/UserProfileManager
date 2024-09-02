package com.example.auth;

import com.example.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

	private String firstname;
	private String lastname;
	private String middlename;
	private LocalDate birthdate;
	private String phoneNumber;
	private String email;
	private String password;
	private Role role;
}
