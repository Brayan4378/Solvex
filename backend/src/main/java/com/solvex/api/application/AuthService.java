package com.solvex.api.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.solvex.api.common.ResourceNotFoundException;
import com.solvex.api.domain.model.Usuario;
import com.solvex.api.dto.AuthDtos;
import com.solvex.api.infrastructure.firestore.FirestoreUsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {
    private final FirestoreUsuarioRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final String googleClientId;
    private final long expirationMinutes;

    public AuthService(FirestoreUsuarioRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
                       @Value("${solvex.google.client-id}") String googleClientId,
                       @Value("${solvex.jwt.expiration-minutes}") long expirationMinutes) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.googleClientId = googleClientId;
        this.expirationMinutes = expirationMinutes;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalize(request.email());
        if (users.findByEmail(email).isPresent()) throw new IllegalArgumentException("El correo ya está registrado");
        Usuario created = users.create(request.nombre(), email, passwordEncoder.encode(request.contrasena()));
        return tokenResponse(created);
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = normalize(request.email());
        Usuario user = users.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos"));

        boolean valid = !user.passwordHash().isBlank() && passwordEncoder.matches(request.contrasena(), user.passwordHash());
        if (!valid) {
            Optional<String> legacy = users.findLegacyPassword(email);
            if (legacy.isPresent() && legacy.get().equals(request.contrasena())) {
                users.upgradePasswordHash(email, passwordEncoder.encode(request.contrasena()));
                valid = true;
            }
        }
        if (!valid) throw new BadCredentialsException("Correo o contraseña incorrectos");
        return tokenResponse(user);
    }

    public AuthDtos.AuthResponse loginWithGoogle(String idToken) {
        if (googleClientId.isBlank()) throw new IllegalStateException("Falta configurar GOOGLE_WEB_CLIENT_ID");
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(List.of(googleClientId)).build();
            GoogleIdToken verified = verifier.verify(idToken);
            if (verified == null || !Boolean.TRUE.equals(verified.getPayload().getEmailVerified())) {
                throw new BadCredentialsException("El token de Google no es válido");
            }
            GoogleIdToken.Payload payload = verified.getPayload();
            String email = normalize(payload.getEmail());
            String nombre = String.valueOf(payload.get("name") == null ? substringBeforeAt(email) : payload.get("name"));
            Usuario user = users.findByEmail(email).orElseGet(() -> users.create(nombre, email, ""));
            return tokenResponse(user);
        } catch (IOException | GeneralSecurityException exception) {
            throw new BadCredentialsException("No se pudo validar la cuenta de Google", exception);
        } catch (IllegalArgumentException exception) {
            throw new BadCredentialsException("El token de Google no es vÃ¡lido", exception);
        }
    }

    public AuthDtos.UserResponse profile(String email) {
        Usuario user = users.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario"));
        return new AuthDtos.UserResponse(user.id(), user.nombre(), user.email());
    }

    public AuthDtos.UserResponse updateProfile(String email, String nombre) {
        String normalizedEmail = normalize(email);
        users.findByEmail(normalizedEmail).orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario"));
        users.updateProfile(normalizedEmail, nombre);
        Usuario updated = users.findByEmail(normalizedEmail).orElseThrow();
        return new AuthDtos.UserResponse(updated.id(), updated.nombre(), updated.email());
    }

    private AuthDtos.AuthResponse tokenResponse(Usuario user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("solvex-api").issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES)).subject(user.email())
                .claim("name", user.nombre()).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthDtos.AuthResponse(token, "Bearer",
                new AuthDtos.UserResponse(user.id(), user.nombre(), user.email()));
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private static String substringBeforeAt(String email) { return email.substring(0, email.indexOf('@')); }
}
