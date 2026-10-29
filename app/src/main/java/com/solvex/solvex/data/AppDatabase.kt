package com.solvex.solvex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// base de datos principal de la app manejada por room
@Database(
    entities = [UsuarioEntity::class, MovimientoEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // acceso a las operaciones de la tabla usuarios
    abstract fun usuarioDao(): UsuarioDao

    // acceso a las operaciones crud de la tabla movimientos
    abstract fun movimientoDao(): MovimientoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // obtener la instancia unica de la base de datos
        fun obtenerInstancia(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "solvex_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instancia
                instancia
            }
        }
    }
}
