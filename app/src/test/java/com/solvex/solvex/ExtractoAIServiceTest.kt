package com.solvex.solvex

import com.solvex.solvex.data.service.ExtractoAIService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractoAIServiceTest {

    private val service = ExtractoAIService()

    @Test
    fun procesaResumenDaviviendaConCaracteresAcentuadosPerdidosYMovimientosInternos() = runBlocking {
        val texto = """
            Saldo Anterior ${'$'}1,000.00
            M\uFFFDs Cr\uFFFDditos ${'$'}150.00
            Menos D\uFFFDbitos ${'$'}125.00
            Nuevo Saldo ${'$'}1,025.00
            EXTRACTO CUENTA DE AHORROS
            01 10 ${'$'} 100.00+ 0001 Transferencia De Otra Entidad A Llave Redeban BreB
            01 10 ${'$'} 50.00+ 0007 Abono Transf Internacional Bancomex BTA PROCESOS ESP.
            02 10 ${'$'} 100.00- 0002 Compra Mercado Pago*MERCADOLI FRANQUICIA MASTER CARD
            03 10 ${'$'} 25.00- 0003 Pago A Llave De Comercio Redeban BreB
            31 10 ${'$'} 58,281.33- 0004 Traslado rendimientos a Bolsillo BTA PROCESOS ESP.
            EXTRACTO BOLSILLO
            31 10 ${'$'} 58,272.35+ 0005 Abono Rendimientos Netos Desde Cuenta BTA PROCESOS ESP.
            31 10 ${'$'} 8.98+ 0006 Abono Rendimientos Netos Desde Cuenta BTA PROCESOS ESP.
        """.trimIndent()

        val result = service.procesarTexto(texto, "test@solvex.com", "Davivienda")

        assertEquals(1_000.0, result.saldoInicial, 0.0)
        assertEquals(1_025.0, result.saldoFinal, 0.0)
        assertEquals(150.0, result.totalIngresosCalculado, 0.0)
        assertEquals(125.0, result.totalGastosCalculado, 0.0)
        assertTrue(result.saldoCuadra)
        assertEquals(4, result.movimientosExtraidos.size)
        assertEquals("Compras y Comercio", result.movimientosExtraidos[2].categoria)
        assertEquals("Compras y Comercio", result.movimientosExtraidos[3].categoria)
    }

    @Test
    fun noInventaSaldoInicialCuandoElExtractoNoLoIncluye() = runBlocking {
        val result = service.procesarTexto(
            "01 10 ${'$'} 50,000.00+ 0001 Transferencia De Otra Entidad A Llave Redeban BreB",
            "test@solvex.com",
            "Davivienda"
        )

        assertEquals(0.0, result.saldoInicial, 0.0)
        assertEquals(50_000.0, result.saldoFinal, 0.0)
    }

    @Test
    fun leeTotalesDeCabeceraCuandoElPdfReemplazaLasLetrasAcentuadas() = runBlocking {
        val reemplazo = '\uFFFD'
        val texto = """
            Saldo Anterior ${'$'}0.00
            M${reemplazo}s Cr${reemplazo}ditos ${'$'}150.00
            Menos D${reemplazo}bitos ${'$'}0.00
            Nuevo Saldo ${'$'}150.00
            01 10 ${'$'} 100.00+ 0001 Transferencia De Otra Entidad A Llave Redeban BreB
        """.trimIndent()

        val result = service.procesarTexto(texto, "test@solvex.com", "Davivienda")

        assertEquals(150.0, result.totalIngresosCalculado, 0.0)
        assertEquals(0.0, result.totalGastosCalculado, 0.0)
        assertEquals(150.0, result.saldoFinal, 0.0)
        assertTrue(result.saldoCuadra)
    }

    @Test
    fun priorizaComerciosEspecificosSobreReglasGenericas() {
        assertEquals(
            "Compras y Comercio",
            service.categorizarConIA("Compra Mercado Pago*MERCADOLI")
        )
        assertEquals(
            "Supermercados y Mercado",
            service.categorizarConIA("Compra SUPERTIENDA OLIMPICA")
        )
        assertEquals(
            "Transporte y Gasolina",
            service.categorizarConIA("Compra EDS TERPEL LA GALAN")
        )
    }

    @Test
    fun categorizaComerciosYMovimientosDelExtractoDaviviendaDeOctubre() {
        val casosDelExtracto = listOf(
            "Compra EDS TERPEL LA GALAN" to "Transporte y Gasolina",
            "Compra EDS URBANOS CIUDAD MIL" to "Transporte y Gasolina",
            "Compra FULLGAS SAN CAMILO" to "Transporte y Gasolina",
            "Compra TEXACO EDS COOBURQUIN" to "Transporte y Gasolina",
            "Compra UBER*RIDES" to "Transporte y Gasolina",
            "Compra SUPERTIENDA OLIMPICA 3" to "Supermercados y Mercado",
            "Compra OXXO VERANERA" to "Supermercados y Mercado",
            "Compra JERONIMO MARTINS COLOM" to "Supermercados y Mercado",
            "Compra Rappi" to "Comida y Restaurantes",
            "Compra SPOTIFY" to "Entretenimiento",
            "Compra AMAZON.COM" to "Compras y Comercio",
            "Compra Mercado Pago*MERCADOLI" to "Compras y Comercio",
            "Compra CENTRO COMERCIAL UNICE" to "Compras y Comercio",
            "Pago Tarj. Credito N4559860026308316" to "Pago de Tarjetas",
            "Pago De Servicio App Davivienda" to "Facturas y Servicios",
            "Transferencia A Llave Otra Entidad Redeban BreB" to "Transferencias y Envíos",
            "Transferencia enviada a Daviplata App Davivienda" to "Transferencias y Envíos",
            "Transferencia de Dinero a Bolsillo App Davivienda" to "Bolsillos y Ahorro",
            "Abono De Bolsillo A Cuenta App Davivienda" to "Bolsillos y Ahorro",
            "Abono Transf Internacional Bancomex BTA PROCESOS ESP." to "Ingresos y Rendimientos",
            "Rendimientos Financieros" to "Ingresos y Rendimientos"
        )

        casosDelExtracto.forEach { (descripcion, categoriaEsperada) ->
            assertEquals(descripcion, categoriaEsperada, service.categorizarConIA(descripcion))
        }
    }
}
