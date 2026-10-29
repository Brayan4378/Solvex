package com.solvex.solvex.data.repository

import com.solvex.solvex.data.MovimientoDao
import com.solvex.solvex.data.MovimientoEntity
import kotlinx.coroutines.flow.Flow

// repositorio que encapsula el acceso a datos de movimientos
class MovimientoRepository(private val dao: MovimientoDao) {

    fun obtenerPorUsuario(email: String): Flow<List<MovimientoEntity>> =
        dao.obtenerPorUsuario(email)

    suspend fun buscarPorId(id: Int): MovimientoEntity? = dao.buscarPorId(id)

    suspend fun insertar(movimiento: MovimientoEntity) = dao.insertar(movimiento)

    suspend fun actualizar(movimiento: MovimientoEntity) = dao.actualizar(movimiento)

    suspend fun eliminar(movimiento: MovimientoEntity) = dao.eliminar(movimiento)

    fun obtenerTotalGastosPorUsuario(email: String): Flow<Double> =
        dao.obtenerTotalGastosPorUsuario(email)

    fun obtenerTotalIngresosPorUsuario(email: String): Flow<Double> =
        dao.obtenerTotalIngresosPorUsuario(email)
}
