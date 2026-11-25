package com.solvex.api.infrastructure.firestore;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.api.core.ApiFuture;
import com.solvex.api.domain.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreUsuarioRepository {
    private final Firestore firestore;
    public FirestoreUsuarioRepository(Firestore firestore) { this.firestore = firestore; }

    public Optional<Usuario> findByEmail(String email) {
        DocumentSnapshot doc = await(firestore.collection("usuarios").document(normalize(email)).get());
        if (!doc.exists()) return Optional.empty();
        return Optional.of(new Usuario(
                doc.getLong("id") == null ? 0 : doc.getLong("id").intValue(),
                doc.getString("nombre") == null ? "" : doc.getString("nombre"),
                doc.getString("email") == null ? normalize(email) : doc.getString("email"),
                doc.getString("passwordHash") == null ? "" : doc.getString("passwordHash")
        ));
    }

    public Optional<String> findLegacyPassword(String email) {
        DocumentSnapshot doc = await(firestore.collection("usuarios").document(normalize(email)).get());
        return Optional.ofNullable(doc.getString("contrasena"));
    }

    public Usuario create(String nombre, String email, String passwordHash) {
        String normalizedEmail = normalize(email);
        Map<String, Object> data = new HashMap<>();
        data.put("id", normalizedEmail.hashCode());
        data.put("nombre", nombre.trim());
        data.put("email", normalizedEmail);
        data.put("passwordHash", passwordHash);
        await(firestore.collection("usuarios").document(normalizedEmail).set(data));
        return new Usuario((int) data.get("id"), nombre.trim(), normalizedEmail, passwordHash);
    }

    public void upgradePasswordHash(String email, String passwordHash) {
        await(firestore.collection("usuarios").document(normalize(email)).update(
                "passwordHash", passwordHash, "contrasena", FieldValue.delete()));
    }

    public void updateProfile(String email, String nombre) {
        await(firestore.collection("usuarios").document(normalize(email)).update("nombre", nombre.trim()));
    }

    private static String normalize(String email) { return email.trim().toLowerCase(); }

    private static <T> T await(ApiFuture<T> future) {
        try { return future.get(); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación de Firestore interrumpida", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("No se pudo completar la operación en Firestore", exception.getCause());
        }
    }
}
