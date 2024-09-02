package com.example.auth;

import com.example.config.JwtService;
import com.example.exception.InvalidCredentialsException;
import com.example.token.Token;
import com.example.token.TokenRepository;
import com.example.token.TokenType;
import com.example.user.User;
import com.example.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
	private final UserRepository repository;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	public AuthenticationResponse register(RegisterRequest request) {
		var user = User.builder()
				.firstname(request.getFirstname())
				.lastname(request.getLastname())
				.email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword()))
				.role(request.getRole())
				.build();
		var savedUser = repository.save(user);
		var jwtToken = jwtService.generateToken(user);
		saveUserToken(savedUser, jwtToken);
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.build();
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		try {
			// Попробуйте аутентифицировать пользователя
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(
							request.getEmail(),
							request.getPassword()
					)
			);
		} catch (Exception e) {
			// Если аутентификация не удалась, выбрасываем исключение
			throw new InvalidCredentialsException("Invalid email or password");
		}

		// Попробуйте найти пользователя в базе данных
		var user = repository.findByEmail(request.getEmail())
				.orElseThrow(() -> new InvalidCredentialsException("User not found"));

		// Генерация JWT токена
		var jwtToken = jwtService.generateToken(user);

		// Отозвать все токены пользователя и сохранить новый токен
		revokeAllUserTokens(user);
		saveUserToken(user, jwtToken);

		// Создание и возврат ответа
		return AuthenticationResponse.builder()
				.accessToken(jwtToken)
				.build();
	}

	private void saveUserToken(User user, String jwtToken) {
		var token = Token.builder()
				.user(user)
				.token(jwtToken)
				.tokenType(TokenType.BEARER)
				.expired(false)
				.revoked(false)
				.build();
		tokenRepository.save(token);
	}

	private void revokeAllUserTokens(User user) {
		var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
		if (validUserTokens.isEmpty())
			return;
		validUserTokens.forEach(token -> {
			token.setExpired(true);
			token.setRevoked(true);
		});
		tokenRepository.saveAll(validUserTokens);
	}
}
