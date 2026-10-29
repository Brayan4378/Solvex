package com.solvex.solvex.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// operaciones de base de datos crud para los gastos y movimientos filtrados por usuario
@Dao
interface MovimientoDao {
    // c de crud (crear): guardar un nuevo movimiento en la base de datos
    @Insert
    suspend fun insertar(movimiento: MovimientoEntity)

    // r de crud (leer): obtener la lista del usuario actual ordenada por fecha
    @Query("SELECT * FROM movimientos WHERE usuarioEmail = :email ORDER BY fecha DESC")
    fun obtenerPorUsuario(email: String): Flow<List<MovimientoEntity>>

    // r de crud (leer): buscar un movimiento especifico por su id
    @Query("SELECT * FROM movimientos WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: Int): MovimientoEntity?

    // u de crud (actualizar): modificar los datos de un movimiento existente
    @Update
    suspend fun actualizar(movimiento: MovimientoEntity)

    // d de crud (eliminar): borrar un movimiento de la base de datos
    @Delete
    suspend fun eliminar(movimiento: MovimientoEntity)

    // suma total de los gastos del usuario actual
    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM movimientos WHERE usuarioEmail = :email AND tipo = 'gasto'")
    fun obtenerTotalGastosPorUsuario(email: String): Flow<Double>

    // suma total de los ingresos del usuario actual
    @Query("SELECT COALESCE(SUM(monto), 0.0) FROM movimientos WHERE usuarioEmail = :email AND tipo = 'ingreso'")
    fun obtenerTotalIngresosPorUsuario(email: String): Flow<Double>
}
