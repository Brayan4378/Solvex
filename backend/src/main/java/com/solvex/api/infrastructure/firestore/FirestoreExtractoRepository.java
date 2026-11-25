package com.solvex.api.infrastructure.firestore;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.solvex.api.domain.model.Extracto;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreExtractoRepository {
    private final Firestore firestore;
    public FirestoreExtractoRepository(Firestore firestore) { this.firestore = firestore; }

    public List<Extracto> findByUser(String email) {
        return await(firestore.collection("extractos").whereEqualTo("userEmail", normalize(email)).get())
                .getDocuments().stream().map(FirestoreExtractoRepository::toDomain)
                .sorted((a, b) -> Long.compare(b.fechaProcesado(), a.fechaProcesado())).limit(20).toList();
    }

    public Extracto create(String email, String nombre, String banco, int cantidad, String estado) {
        String normalizedEmail = normalize(email);
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", normalizedEmail);
        data.put("nombreArchivo", nombre);
        data.put("banco", banco);
        data.put("fechaProcesado", now);
        data.put("totalMovimientos", cantidad);
        data.put("estado", estado == null || estado.isBlank() ? "Procesado" : estado);
        await(firestore.collection("extractos").document(id).set(data));
        return new Extracto(id, normalizedEmail, nombre, banco, now, cantidad, (String) data.get("estado"));
    }

    public void deleteAll(String email) {
        var docs = await(firestore.collection("extractos").whereEqualTo("userEmail", normalize(email)).get())
                .getDocuments();
        for (int start = 0; start < docs.size(); start += 400) {
            var batch = firestore.batch();
            docs.subList(start, Math.min(start + 400, docs.size())).forEach(doc -> batch.delete(doc.getReference()));
            await(batch.commit());
        }
    }

    private static Extracto toDomain(QueryDocumentSnapshot doc) {
        return new Extracto(doc.getId(), doc.getString("userEmail"),
                doc.getString("nombreArchivo") == null ? "" : doc.getString("nombreArchivo"),
                doc.getString("banco") == null ? "" : doc.getString("banco"),
                doc.getLong("fechaProcesado") == null ? 0 : doc.getLong("fechaProcesado"),
                doc.getLong("totalMovimientos") == null ? 0 : doc.getLong("totalMovimientos").intValue(),
                doc.getString("estado") == null ? "Procesado" : doc.getString("estado"));
    }

    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static <T> T await(com.google.api.core.ApiFuture<T> future) {
        try { return future.get(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
        catch (ExecutionException exception) { throw new IllegalStateException("No se pudo consultar Firestore", exception.getCause()); }
    }
}
