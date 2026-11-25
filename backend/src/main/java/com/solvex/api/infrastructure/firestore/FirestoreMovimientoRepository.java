package com.solvex.api.infrastructure.firestore;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.solvex.api.domain.model.Movimiento;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreMovimientoRepository {
    private final Firestore firestore;
    public FirestoreMovimientoRepository(Firestore firestore) { this.firestore = firestore; }

    public List<Movimiento> findByUser(String email) {
        return await(firestore.collection("movimientos").whereEqualTo("usuarioEmail", normalize(email)).get())
                .getDocuments().stream().map(FirestoreMovimientoRepository::toDomain)
                .sorted((a, b) -> Long.compare(b.fecha(), a.fecha())).toList();
    }

    public Movimiento create(String email, Movimiento input) {
        String docId = UUID.randomUUID().toString();
        int id = docId.hashCode();
        Movimiento saved = new Movimiento(id, normalize(email), input.titulo(), input.monto(),
                input.categoria(), input.tipo(), input.fecha());
        await(firestore.collection("movimientos").document(docId).set(toMap(saved)));
        return saved;
    }

    public Movimiento update(String email, int id, Movimiento input) {
        QueryDocumentSnapshot doc = findDocument(email, id);
        Movimiento updated = new Movimiento(id, normalize(email), input.titulo(), input.monto(),
                input.categoria(), input.tipo(), input.fecha());
        await(doc.getReference().set(toMap(updated)));
        return updated;
    }

    public void delete(String email, int id) {
        QueryDocumentSnapshot doc = findDocument(email, id);
        await(doc.getReference().delete());
    }

    public void deleteAll(String email) {
        List<QueryDocumentSnapshot> docs = await(firestore.collection("movimientos")
                .whereEqualTo("usuarioEmail", normalize(email)).get()).getDocuments();
        for (int start = 0; start < docs.size(); start += 400) {
            WriteBatch batch = firestore.batch();
            docs.subList(start, Math.min(start + 400, docs.size())).forEach(doc -> batch.delete(doc.getReference()));
            await(batch.commit());
        }
    }

    /** Replaces matching imported rows idempotently while preserving identical transactions as a multiset. */
    public List<Movimiento> saveImported(String email, List<Movimiento> incoming) {
        String normalizedEmail = normalize(email);
        Set<String> signatures = new HashSet<>();
        incoming.forEach(row -> signatures.add(signature(normalizedEmail, row)));
        List<QueryDocumentSnapshot> existing = await(firestore.collection("movimientos")
                .whereEqualTo("usuarioEmail", normalizedEmail).get()).getDocuments();
        List<QueryDocumentSnapshot> toDelete = existing.stream()
                .filter(doc -> signatures.contains(signature(normalizedEmail, toDomain(doc)))).toList();
        for (int start = 0; start < toDelete.size(); start += 400) {
            WriteBatch batch = firestore.batch();
            toDelete.subList(start, Math.min(start + 400, toDelete.size()))
                    .forEach(doc -> batch.delete(doc.getReference()));
            await(batch.commit());
        }

        Map<String, Integer> occurrences = new HashMap<>();
        List<Movimiento> saved = new ArrayList<>(incoming.size());
        for (int start = 0; start < incoming.size(); start += 400) {
            WriteBatch batch = firestore.batch();
            for (Movimiento row : incoming.subList(start, Math.min(start + 400, incoming.size()))) {
                String signature = signature(normalizedEmail, row);
                int occurrence = occurrences.merge(signature, 1, Integer::sum);
                String docId = sha256(signature + "#" + occurrence);
                int id = docId.hashCode();
                Movimiento entity = new Movimiento(id, normalizedEmail, row.titulo(), row.monto(),
                        row.categoria(), row.tipo(), row.fecha());
                batch.set(firestore.collection("movimientos").document(docId), toMap(entity));
                saved.add(entity);
            }
            await(batch.commit());
        }
        return saved;
    }

    private QueryDocumentSnapshot findDocument(String email, int id) {
        List<QueryDocumentSnapshot> docs = await(firestore.collection("movimientos")
                .whereEqualTo("usuarioEmail", normalize(email)).whereEqualTo("id", id).get()).getDocuments();
        if (docs.isEmpty()) throw new IllegalArgumentException("No se encontró el movimiento solicitado");
        return docs.get(0);
    }

    private static Movimiento toDomain(DocumentSnapshot doc) {
        return new Movimiento(doc.getLong("id") == null ? doc.getId().hashCode() : doc.getLong("id").intValue(),
                doc.getString("usuarioEmail") == null ? "" : doc.getString("usuarioEmail"),
                doc.getString("titulo") == null ? "" : doc.getString("titulo"),
                doc.getDouble("monto") == null ? 0.0 : doc.getDouble("monto"),
                doc.getString("categoria") == null ? "Otro" : doc.getString("categoria"),
                doc.getString("tipo") == null ? "gasto" : doc.getString("tipo"),
                doc.getLong("fecha") == null ? 0L : doc.getLong("fecha"));
    }

    private static Map<String, Object> toMap(Movimiento movement) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", movement.id());
        data.put("usuarioEmail", movement.usuarioEmail());
        data.put("titulo", movement.titulo());
        data.put("monto", movement.monto());
        data.put("categoria", movement.categoria());
        data.put("tipo", movement.tipo());
        data.put("fecha", movement.fecha());
        return data;
    }

    private static String signature(String email, Movimiento movement) {
        return normalize(email) + "|" + movement.titulo().trim().toLowerCase(Locale.ROOT) + "|" +
                movement.monto() + "|" + movement.fecha() + "|" + movement.tipo().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte item : digest) hex.append(String.format("%02x", item));
            return hex.toString();
        } catch (Exception exception) { throw new IllegalStateException("No se pudo generar la firma", exception); }
    }

    private static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }

    private static <T> T await(com.google.api.core.ApiFuture<T> future) {
        try { return future.get(); }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operación de Firestore interrumpida", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("No se pudo completar la operación en Firestore", exception.getCause());
        }
    }
}
