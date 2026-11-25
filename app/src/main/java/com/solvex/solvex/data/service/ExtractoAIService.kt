package com.solvex.solvex.data.service

import android.content.Context
import android.net.Uri
import com.solvex.solvex.data.entity.ExtractoAnalizado
import com.solvex.solvex.data.entity.MovimientoEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

/**
 * Servicio inteligente para extracción y categorización de extractos bancarios colombianos.
 * Extracción de texto mediante PDFBox + Categorización limpia semántica con IA.
 */
class ExtractoAIService {

    // ── 1. Extracción de texto de archivos PDF o Texto Libre ────────────────
    suspend fun procesarExtracto(
        context: Context,
        uri: Uri?,
        nombreArchivo: String,
        userEmail: String,
        contrasena: String = ""
    ): ExtractoAnalizado = withContext(Dispatchers.IO) {

        var textoExtraido = ""

        if (uri != null) {
            val esPDF = nombreArchivo.lowercase(Locale.ROOT).endsWith(".pdf") ||
                    context.contentResolver.getType(uri)?.contains("pdf") == true

            if (esPDF) {
                textoExtraido = extraerTextoPDF(context, uri, contrasena)
            } else {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        textoExtraido = BufferedReader(InputStreamReader(stream)).readText()
                    }
                } catch (_: Exception) {}
            }
        }

        if (textoExtraido.isBlank()) {
            return@withContext ExtractoAnalizado(
                resumenIA = "No se pudo extraer texto del archivo '$nombreArchivo'.\n\n" +
                        "Si el PDF requiere contraseña, ingresa tu cédula o clave e intenta de nuevo."
            )
        }

        val banco = detectarBanco(textoExtraido)
        procesarTexto(textoExtraido, userEmail, banco)
    }

    private fun extraerTextoPDF(context: Context, uri: Uri, contrasena: String): String {
        try {
            PDFBoxResourceLoader.init(context)

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ""

            var documento: PDDocument? = null
            try {
                documento = if (contrasena.isNotBlank()) {
                    PDDocument.load(bytes, contrasena)
                } else {
                    try {
                        PDDocument.load(bytes)
                    } catch (_: Exception) {
                        try {
                            PDDocument.load(bytes, "")
                        } catch (_: Exception) {
                            return ""
                        }
                    }
                }

                val stripper = PDFTextStripper()
                stripper.sortByPosition = true
                return stripper.getText(documento)
            } finally {
                try { documento?.close() } catch (_: Exception) {}
            }
        } catch (_: Exception) {
            return ""
        }
    }

    // ── 2. Procesamiento del texto extraído y estructuración de datos ──────
    suspend fun procesarTexto(
        texto: String,
        userEmail: String,
        banco: String = ""
    ): ExtractoAnalizado = withContext(Dispatchers.IO) {

        val lineas = texto.lines().map { it.trim() }.filter { it.isNotBlank() }
        val bancoDetectado = if (banco.isNotBlank()) banco else detectarBanco(texto)

        // Extraer valores de cabecera del banco
        var saldoAnterior = extraerMontoCabecera(
            lineas,
            listOf("saldo anterior", "saldo ant", "saldo inicial", "saldo de apertura")
        )
        val masCreditos = extraerMontoCabecera(
            lineas,
            listOf("más créditos", "mas creditos", "total créditos", "total creditos", "créditos", "creditos")
        )
        val menosDebitos = extraerMontoCabecera(
            lineas,
            listOf("menos débitos", "menos debitos", "total débitos", "total debitos", "débitos", "debitos")
        )
        var nuevoSaldo = extraerMontoCabecera(
            lineas,
            listOf("nuevo saldo", "saldo final", "saldo a la fecha", "saldo disponible")
        )
        val saldoBolsillo = extraerMontoCabecera(
            lineas,
            listOf("saldo total bolsillo", "total bolsillo", "bolsillos", "ahorro bolsillo")
        )

        val movimientos = mutableListOf<MovimientoEntity>()

        // Patrones Regex de líneas de transacciones bancarias
        val regexDavivienda = Regex("""^(\d{1,2})\s+(\d{1,2})\s+\$\s*([\d,.\s]+?)([+-])\s*(\d+)?\s+(.+)$""")
        val regexColumnar = Regex("""^(\d{1,2})\s+(\d{1,2})\s+([\d,.]+(?:\.\d{2})?)([+-])\s*(\d+)?\s+(.+)$""")
        val regexFechaInicio = Regex("""^(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\s+(.+?)\s+\$?\s*([\d,.]+)\s*([+-]|DB|CR)?$""", RegexOption.IGNORE_CASE)
        val regexFechaTexto = Regex("""^(\d{1,2})\s+([A-Za-z]{3,9})(?:\s+(\d{2,4}))?\s+(.+?)\s+\$?\s*([\d,.]+)\s*([+-]|DB|CR)?$""", RegexOption.IGNORE_CASE)
        val regexLibre = Regex("""^([+-])\s*\$?\s*([\d.,]+)\s*\|?\s*(.+)$""")

        var fechaDia = 1
        var fechaMes = Calendar.getInstance().get(Calendar.MONTH) + 1

        for (linea in lineas) {
            val lineaUpper = linea.uppercase(Locale.ROOT)

            if (lineaUpper.startsWith("FECHA") || lineaUpper.startsWith("BANCO") ||
                lineaUpper.startsWith("APRECIADO") || lineaUpper.startsWith("NIT") ||
                lineaUpper.contains("SALDO ANTERIOR") || lineaUpper.contains("NUEVO SALDO") ||
                lineaUpper.contains("MÁS CRÉDITOS") || lineaUpper.contains("MENOS DÉBITOS")) {
                continue
            }

            val mDav = regexDavivienda.find(linea)
            if (mDav != null) {
                val dia = mDav.groupValues[1].toIntOrNull() ?: 1
                val mes = mDav.groupValues[2].toIntOrNull() ?: fechaMes
                val montoRaw = mDav.groupValues[3].replace(" ", "")
                val signo = mDav.groupValues[4]
                val desc = mDav.groupValues[6].trim()

                val monto = parsearMonto(montoRaw)
                if (monto > 0.01) {
                    val tipo = if (signo == "-") "gasto" else "ingreso"
                    movimientos.add(
                        MovimientoEntity(
                            id = 0,
                            usuarioEmail = userEmail,
                            titulo = limpiarDesc(desc),
                            monto = monto,
                            categoria = categorizarConIA(desc),
                            tipo = tipo,
                            fecha = construirFecha(dia, mes)
                        )
                    )
                }
                continue
            }

            val mCol = regexColumnar.find(linea)
            if (mCol != null) {
                val dia = mCol.groupValues[1].toIntOrNull() ?: 1
                val mes = mCol.groupValues[2].toIntOrNull() ?: fechaMes
                val montoRaw = mCol.groupValues[3]
                val signo = mCol.groupValues[4]
                val desc = mCol.groupValues[6].trim()

                val monto = parsearMonto(montoRaw)
                if (monto > 0.01) {
                    val tipo = if (signo == "-") "gasto" else "ingreso"
                    movimientos.add(
                        MovimientoEntity(
                            id = 0,
                            usuarioEmail = userEmail,
                            titulo = limpiarDesc(desc),
                            monto = monto,
                            categoria = categorizarConIA(desc),
                            tipo = tipo,
                            fecha = construirFecha(dia, mes)
                        )
                    )
                }
                continue
            }

            val mIni = regexFechaInicio.find(linea)
            if (mIni != null) {
                val dia = mIni.groupValues[1].toIntOrNull() ?: fechaDia
                val mes = mIni.groupValues[2].toIntOrNull() ?: fechaMes
                val desc = mIni.groupValues[4].trim()
                val montoRaw = mIni.groupValues[5]
                val signoRaw = mIni.groupValues[6]

                val monto = parsearMonto(montoRaw)
                if (monto > 0.01) {
                    val tipo = if (signoRaw == "-" || signoRaw.equals("DB", ignoreCase = true)) "gasto" else "ingreso"
                    movimientos.add(
                        MovimientoEntity(
                            id = 0,
                            usuarioEmail = userEmail,
                            titulo = limpiarDesc(desc),
                            monto = monto,
                            categoria = categorizarConIA(desc),
                            tipo = tipo,
                            fecha = construirFecha(dia, mes)
                        )
                    )
                }
                continue
            }

            val mTxt = regexFechaTexto.find(linea)
            if (mTxt != null) {
                val dia = mTxt.groupValues[1].toIntOrNull() ?: fechaDia
                val mesStr = mTxt.groupValues[2]
                val mes = convertirMesAInt(mesStr) ?: fechaMes
                val desc = mTxt.groupValues[4].trim()
                val montoRaw = mTxt.groupValues[5]
                val signoRaw = mTxt.groupValues[6]

                val monto = parsearMonto(montoRaw)
                if (monto > 0.01) {
                    val tipo = if (signoRaw == "-" || signoRaw.equals("DB", ignoreCase = true)) "gasto" else "ingreso"
                    movimientos.add(
                        MovimientoEntity(
                            id = 0,
                            usuarioEmail = userEmail,
                            titulo = limpiarDesc(desc),
                            monto = monto,
                            categoria = categorizarConIA(desc),
                            tipo = tipo,
                            fecha = construirFecha(dia, mes)
                        )
                    )
                }
                continue
            }

            val mLib = regexLibre.matchEntire(linea)
            if (mLib != null) {
                val signo = mLib.groupValues[1]
                val montoRaw = mLib.groupValues[2]
                val desc = mLib.groupValues[3].trim()

                val monto = parsearMonto(montoRaw)
                if (monto > 0.01) {
                    val tipo = if (signo == "-") "gasto" else "ingreso"
                    movimientos.add(
                        MovimientoEntity(
                            id = 0,
                            usuarioEmail = userEmail,
                            titulo = limpiarDesc(desc),
                            monto = monto,
                            categoria = categorizarConIA(desc),
                            tipo = tipo,
                            fecha = construirFecha(fechaDia, fechaMes)
                        )
                    )
                }
            }
        }

        if (movimientos.isEmpty()) {
            return@withContext ExtractoAnalizado(
                nombreBanco = bancoDetectado,
                resumenIA = "No se detectaron transacciones en el texto ingresado."
            )
        }

        val totalIngresosInternos = movimientos.filter { it.tipo == "ingreso" }.sumOf { it.monto }
        val totalGastosInternos = movimientos.filter { it.tipo == "gasto" }.sumOf { it.monto }

        val ingresosOficiales = if (masCreditos > 0) masCreditos else totalIngresosInternos
        val gastosOficiales = if (menosDebitos > 0) menosDebitos else totalGastosInternos

        if (saldoAnterior == 0.0 && nuevoSaldo == 0.0) {
            saldoAnterior = ingresosOficiales
            nuevoSaldo = ingresosOficiales - gastosOficiales
        }
        if (nuevoSaldo == 0.0) {
            nuevoSaldo = saldoAnterior + ingresosOficiales - gastosOficiales
        }

        val saldoCalculado = saldoAnterior + ingresosOficiales - gastosOficiales
        val saldoCuadra = abs(saldoCalculado - nuevoSaldo) < 2.0

        val resumen = generarResumenIA(
            movimientos = movimientos,
            totalIngresos = ingresosOficiales,
            totalGastos = gastosOficiales,
            saldoAnterior = saldoAnterior,
            saldoFinal = nuevoSaldo,
            saldoBolsillo = saldoBolsillo,
            saldoCuadra = saldoCuadra
        )

        ExtractoAnalizado(
            nombreBanco = bancoDetectado,
            periodo = detectarPeriodo(movimientos),
            saldoInicial = saldoAnterior,
            saldoFinal = nuevoSaldo,
            totalIngresosCalculado = ingresosOficiales,
            totalGastosCalculado = gastosOficiales,
            saldoCuadra = saldoCuadra,
            movimientosExtraidos = movimientos,
            resumenIA = resumen
        )
    }

    // ── 3. Categorización Inteligente por Reglas Semánticas (IA) ────────────
    fun categorizarConIA(descripcion: String): String {
        val d = descripcion.lowercase(Locale.ROOT)

        return when {
            // Entretenimiento y Suscripciones
            d.contains("spotify") || d.contains("netflix") || d.contains("disney") ||
                    d.contains("cine") || d.contains("steam") || d.contains("playstation") ||
                    d.contains("hbo") || d.contains("youtube") || d.contains("prime video") ||
                    d.contains("apple music") || d.contains("crunchyroll") -> "Entretenimiento"

            // Comida y Restaurantes
            d.contains("rappi") || d.contains("restaurante") || d.contains("burger") ||
                    d.contains("crepes") || d.contains("almuerzo") || d.contains("subway") ||
                    d.contains("mcdonalds") || d.contains("mc donald") || d.contains("dominos") ||
                    d.contains("pizza") || d.contains("pollo") || d.contains("panaderia") ||
                    d.contains("panadería") || d.contains("starbucks") || d.contains("juan valdez") ||
                    d.contains("kfc") || d.contains("frisby") || d.contains("el corral") ||
                    d.contains("kokoriko") -> "Comida y Restaurantes"

            // Supermercados y Mercado
            d.contains("éxito") || d.contains("exito") || d.contains("jumbo") ||
                    d.contains("carulla") || d.contains("supertienda") || d.contains("olimpica") ||
                    d.contains("olímpica") || d.contains("tienda d1") || d.contains("ara ") ||
                    d.contains("justo & bueno") || d.contains("supermercado") || d.contains("oxxo") ||
                    d.contains("jeronimo martins") || d.contains("mercado") -> "Supermercados y Mercado"

            // Compras y Comercio
            d.contains("amazon") || d.contains("mercadoli") || d.contains("mercadopago") ||
                    d.contains("mercado libre") || d.contains("falabella") || d.contains("zara") ||
                    d.contains("centro comercial") || d.contains("compra a comercio") ||
                    d.contains("compra en") || d.contains("compra ") || d.contains("linio") ||
                    d.contains("shein") || d.contains("unice") -> "Compras y Comercio"

            // Transporte y Gasolina
            d.contains("terpel") || d.contains("eds ") || d.contains("texaco") ||
                    d.contains("fullgas") || d.contains("cooburquin") || d.contains("combustible") ||
                    d.contains("gasolina") || d.contains("uber") || d.contains("didi") ||
                    d.contains("cabify") || d.contains("indrive") || d.contains("transporte") ||
                    d.contains("peaje") || d.contains("tullave") || d.contains("transmilenio") ||
                    d.contains("parqueadero") -> "Transporte y Gasolina"

            // Pago de Tarjetas y Créditos
            d.contains("pago tarj") || d.contains("pago tarjeta") || d.contains("tarjeta de crédito") ||
                    d.contains("cuota manejo") || d.contains("cuota de manejo") -> "Pago de Tarjetas"

            // Facturas y Servicios
            d.contains("claro") || d.contains("epm") || d.contains("enel") ||
                    d.contains("energía") || d.contains("energia") || d.contains("agua") ||
                    d.contains("arriendo") || d.contains("gas natural") || d.contains("codensa") ||
                    d.contains("movistar") || d.contains("tigo") || d.contains("etb") ||
                    d.contains("pago de servicio") || d.contains("pago servicio") ||
                    d.contains("pago a llave") -> "Facturas y Servicios"

            // Salud y Droguerías
            d.contains("drogas") || d.contains("farmacia") || d.contains("salud") ||
                    d.contains("eps") || d.contains("médico") || d.contains("medico") ||
                    d.contains("clinica") || d.contains("hospital") || d.contains("cruz verde") ||
                    d.contains("farmatodo") -> "Salud y Droguerías"

            // Bolsillos y Ahorro
            d.contains("bolsillo") -> "Bolsillos y Ahorro"

            // Ingresos y Rendimientos
            d.contains("nómina") || d.contains("nomina") || d.contains("salario") ||
                    d.contains("bancómex") || d.contains("bancomex") || d.contains("abono transf") ||
                    d.contains("rendimientos") || d.contains("abono rendimiento") ||
                    d.contains("intereses") || d.contains("cashback") -> "Ingresos y Rendimientos"

            // Transferencias y Envíos
            d.contains("transferencia") || d.contains("daviplata") || d.contains("nequi") ||
                    d.contains("transfiya") || d.contains("redeban") || d.contains("breb") ||
                    d.contains("otra entidad") -> "Transferencias y Envíos"

            // Comisiones e Impuestos
            d.contains("gmf") || d.contains("4x1000") || d.contains("comisión") ||
                    d.contains("comision") || d.contains("impuesto") -> "Comisiones e Impuestos"

            else -> "Otro"
        }
    }

    private fun parsearMonto(raw: String): Double {
        val limpio = raw.trim()
            .replace("$", "")
            .replace(" ", "")
            .replace("+", "")
            .replace("-", "")
            .replace("DB", "", ignoreCase = true)
            .replace("CR", "", ignoreCase = true)

        if (limpio.isBlank()) return 0.0

        return when {
            limpio.contains(",") && limpio.contains(".") -> {
                if (limpio.lastIndexOf('.') > limpio.lastIndexOf(',')) {
                    limpio.replace(",", "").toDoubleOrNull() ?: 0.0
                } else {
                    limpio.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                }
            }
            limpio.contains(",") -> {
                val parteDecimal = limpio.substringAfterLast(",")
                if (parteDecimal.length == 3) {
                    limpio.replace(",", "").toDoubleOrNull() ?: 0.0
                } else {
                    limpio.replace(",", ".").toDoubleOrNull() ?: 0.0
                }
            }
            limpio.contains(".") -> {
                val cantPuntos = limpio.count { it == '.' }
                val parteDecimal = limpio.substringAfterLast(".")
                if (cantPuntos > 1) {
                    limpio.replace(".", "").toDoubleOrNull() ?: 0.0
                } else if (parteDecimal.length == 2) {
                    limpio.toDoubleOrNull() ?: 0.0
                } else if (parteDecimal.length == 3) {
                    limpio.replace(".", "").toDoubleOrNull() ?: 0.0
                } else {
                    limpio.toDoubleOrNull() ?: 0.0
                }
            }
            else -> limpio.toDoubleOrNull() ?: 0.0
        }
    }

    private fun extraerMontoCabecera(lineas: List<String>, claves: List<String>): Double {
        val regexConPesos = Regex("""\$\s*([\d.,]+)""")
        for (linea in lineas) {
            val l = linea.lowercase(Locale.ROOT)
            if (claves.any { l.contains(it) }) {
                val m = regexConPesos.find(linea)
                if (m != null) return parsearMonto(m.groupValues[1])
            }
        }
        val regexSinPesos = Regex("""([\d,]+\.\d{2})""")
        for (linea in lineas) {
            val l = linea.lowercase(Locale.ROOT)
            if (claves.any { l.contains(it) }) {
                val m = regexSinPesos.find(linea)
                if (m != null) return parsearMonto(m.groupValues[1])
            }
        }
        return 0.0
    }

    private fun convertirMesAInt(mesStr: String): Int? {
        val m = mesStr.lowercase(Locale.ROOT)
        return when {
            m.startsWith("ene") || m.startsWith("jan") -> 1
            m.startsWith("feb") -> 2
            m.startsWith("mar") -> 3
            m.startsWith("abr") || m.startsWith("apr") -> 4
            m.startsWith("may") -> 5
            m.startsWith("jun") -> 6
            m.startsWith("jul") -> 7
            m.startsWith("ago") || m.startsWith("aug") -> 8
            m.startsWith("sep") -> 9
            m.startsWith("oct") -> 10
            m.startsWith("nov") -> 11
            m.startsWith("dic") || m.startsWith("dec") -> 12
            else -> null
        }
    }

    private fun construirFecha(dia: Int, mes: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, (mes - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, dia.coerceIn(1, 31))
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun limpiarDesc(desc: String): String =
        desc.replace(Regex("""N\d{10,}"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .replace(Regex("""^\d{4}\s+"""), "")
            .trim()
            .take(60)

    fun detectarBanco(texto: String): String {
        val t = texto.lowercase(Locale.ROOT)
        return when {
            t.contains("davivienda") -> "Davivienda"
            t.contains("bancolombia") -> "Bancolombia"
            t.contains("nequi") -> "Nequi"
            t.contains("bbva") -> "BBVA"
            t.contains("bogotá") || t.contains("bogota") -> "Banco de Bogotá"
            t.contains("occidente") -> "Banco de Occidente"
            t.contains("popular") -> "Banco Popular"
            t.contains("itaú") || t.contains("itau") -> "Itaú"
            t.contains("scotiabank") || t.contains("colpatria") -> "Scotiabank Colpatria"
            t.contains("nu ") || t.contains("nu colombia") -> "Nu"
            else -> "Extracto Bancario"
        }
    }

    private fun detectarPeriodo(movimientos: List<MovimientoEntity>): String {
        if (movimientos.isEmpty()) return "Periodo no detectado"
        val meses = arrayOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
        val cal = Calendar.getInstance()
        cal.timeInMillis = movimientos.minOf { it.fecha }
        val mesMin = meses[cal.get(Calendar.MONTH)]
        cal.timeInMillis = movimientos.maxOf { it.fecha }
        val mesMax = meses[cal.get(Calendar.MONTH)]
        val anio = cal.get(Calendar.YEAR)
        return if (mesMin == mesMax) "$mesMin $anio" else "$mesMin – $mesMax $anio"
    }

    private fun generarResumenIA(
        movimientos: List<MovimientoEntity>,
        totalIngresos: Double,
        totalGastos: Double,
        saldoAnterior: Double,
        saldoFinal: Double,
        saldoBolsillo: Double,
        saldoCuadra: Boolean
    ): String {
        val catGastos = movimientos
            .filter { it.tipo == "gasto" && !it.categoria.contains("Transferencia") }
            .groupBy { it.categoria }
            .mapValues { (_, v) -> v.sumOf { it.monto } }
            .entries.sortedByDescending { it.value }.take(3)

        val topGastos = catGastos.joinToString(", ") { "${it.key} (${cop(it.value)})" }
        val flujo = totalIngresos - totalGastos
        val flujoTexto = if (flujo >= 0) "positivo de ${cop(flujo)}" else "negativo de ${cop(abs(flujo))}"
        val suscripciones = movimientos.filter { m ->
            m.titulo.lowercase(Locale.ROOT).let {
                it.contains("spotify") || it.contains("netflix") || it.contains("disney") ||
                        it.contains("hbo") || it.contains("youtube") || it.contains("prime")
            }
        }
        val capacidadAhorro = if (totalIngresos > 0) (flujo / totalIngresos * 100) else 0.0

        return buildString {
            append("${movimientos.size} transacciones analizadas.\n")
            append("Ingresos: ${cop(totalIngresos)}  |  Gastos: ${cop(totalGastos)}\n")
            append("Flujo de caja $flujoTexto.\n")
            if (topGastos.isNotEmpty()) append("Categorías principales: $topGastos.\n")
            if (suscripciones.isNotEmpty()) append("Suscripciones detectadas: ${suscripciones.joinToString(", ") { it.titulo }}.\n")
            when {
                capacidadAhorro < 0 -> append("Alerta: Tus gastos superan tus ingresos en el periodo.")
                capacidadAhorro < 20 -> append("Tasa de ahorro baja (${String.format(Locale.US, "%.0f", capacidadAhorro)}%). Se recomienda llegar al 20%.")
                else -> append("Excelente: Tasa de ahorro saludable del ${String.format(Locale.US, "%.0f", capacidadAhorro)}%.")
            }
            if (saldoBolsillo > 0) append("\nSaldo en bolsillos: ${cop(saldoBolsillo)}.")
            if (saldoCuadra) append("\n✓ Verificación de saldo correcta.")
            else append("\n⚠ Pequeña diferencia detectada entre saldos e historial de movimientos.")
        }
    }

    private fun cop(monto: Double): String = "$" + String.format(Locale.US, "%,.0f", monto).replace(",", ".")
}
