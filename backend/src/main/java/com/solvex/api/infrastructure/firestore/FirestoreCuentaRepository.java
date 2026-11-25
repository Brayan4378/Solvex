package com.solvex.api.infrastructure.firestore;

import com.google.cloud.firestore.Firestore;
import com.solvex.api.domain.model.Cuenta;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@Repository
public class FirestoreCuentaRepository {
    private final Firestore firestore;
    public FirestoreCuentaRepository(Firestore firestore) { this.firestore = firestore; }

    public Optional<Cuenta> find(String email) {
        var doc = await(firestore.collection("cuentas").document(normalize(email)).get());
        if (!doc.exists()) return Optional.empty();
        return Optional.of(new Cuenta(normalize(email), number(doc.getDouble("saldoInicial")),
                number(doc.getDouble("saldoFinal")), number(doc.getDouble("totalIngresos")),
                number(doc.getDouble("totalGastos")), doc.getString("banco") == null ? "" : doc.getString("banco")));
    }

    public Cuenta save(String email, Cuenta cuenta) {
        String normalizedEmail = normalize(email);
        Map<String, Object> data = new HashMap<>();
        data.put("userEmail", normalizedEmail);
        data.put("saldoInicial", cuenta.saldoInicial());
        data.put("saldoFinal", cuenta.saldoFinal());
        data.put("totalIngresos", cuenta.totalIngresos());
        data.put("totalGastos", cuenta.totalGastos());
        data.put("banco", cuenta.banco() == null ? "" : cuenta.banco());
        await(firestore.collection("cuentas").document(normalizedEmail).set(data));
        return new Cuenta(normalizedEmail, cuenta.saldoInicial(), cuenta.saldoFinal(),
                cuenta.totalIngresos(), cuenta.totalGastos(), cuenta.banco());
    }

    public void delete(String email) {
        await(firestore.collection("cuentas").document(normalize(email)).delete());
    }

    private static double number(Double value) { return value == null ? 0.0 : value; }
    private static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static <T> T await(com.google.api.core.ApiFuture<T> future) {
        try { return future.get(); }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
        catch (ExecutionException exception) { throw new IllegalStateException("No se pudo consultar Firestore", exception.getCause()); }
    }
}
