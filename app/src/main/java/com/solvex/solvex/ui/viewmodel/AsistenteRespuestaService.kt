package com.solvex.solvex.ui.viewmodel

import com.solvex.solvex.data.entity.MovimientoEntity
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Respuestas basadas exclusivamente en los movimientos recibidos del repositorio. */
internal class AsistenteRespuestaService {

    fun responder(pregunta: String, movimientos: List<MovimientoEntity>, userName: String): String {
        val consulta = normalizar(pregunta)
        val nombre = userName.trim().substringBefore(' ').takeIf { it.isNotBlank() }
        val saludo = nombre?.let { "$it, " } ?: ""
        if (movimientos.isEmpty()) {
            return "Aún no encuentro movimientos registrados. Cuando importes un extracto o registres movimientos, podré responder con esos datos."
        }

        val fechaReferencia = movimientos.maxOf { it.fecha }
        val referencia = Calendar.getInstance().apply { timeInMillis = fechaReferencia }
        val mesReferencia = referencia.get(Calendar.MONTH)
        val anioReferencia = referencia.get(Calendar.YEAR)
        val esMesAnterior = consulta.contains("mes pasado") || consulta.contains("mes anterior")
        val mesConsultado = when {
            esMesAnterior -> (mesReferencia + 11) % 12
            consulta.contains("enero") -> Calendar.JANUARY
            consulta.contains("febrero") -> Calendar.FEBRUARY
            consulta.contains("marzo") -> Calendar.MARCH
            consulta.contains("abril") -> Calendar.APRIL
            consulta.contains("mayo") -> Calendar.MAY
            consulta.contains("junio") -> Calendar.JUNE
            consulta.contains("julio") -> Calendar.JULY
            consulta.contains("agosto") -> Calendar.AUGUST
            consulta.contains("septiembre") -> Calendar.SEPTEMBER
            consulta.contains("octubre") -> Calendar.OCTOBER
            consulta.contains("noviembre") -> Calendar.NOVEMBER
            consulta.contains("diciembre") -> Calendar.DECEMBER
            else -> mesReferencia
        }
        val anioConsultado = if (esMesAnterior && mesReferencia == Calendar.JANUARY) anioReferencia - 1 else anioReferencia
        val todoHistorial = listOf("historial", "todo el tiempo", "todos los movimientos").any { consulta.contains(it) }
        val delPeriodo = if (todoHistorial) movimientos else movimientos.filter { movimiento ->
            Calendar.getInstance().apply { timeInMillis = movimiento.fecha }.let {
                it.get(Calendar.MONTH) == mesConsultado && it.get(Calendar.YEAR) == anioConsultado
            }
        }
        val gastos = delPeriodo.filter { it.tipo.equals("gasto", ignoreCase = true) }
        val ingresos = delPeriodo.filter { it.tipo.equals("ingreso", ignoreCase = true) && !normalizar(it.categoria).contains("bolsillo") }
        // Mover dinero a un bolsillo propio no es consumo ni ahorro adicional del periodo.
        val gastosConsumo = gastos.filterNot { normalizar(it.categoria).contains("bolsillo") }
        val totalGastos = gastosConsumo.sumOf { it.monto }
        val totalIngresos = ingresos.sumOf { it.monto }
        val periodo = if (todoHistorial) "en todo tu historial" else {
            val mes = SimpleDateFormat("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))
                .format(Calendar.getInstance().apply { set(anioConsultado, mesConsultado, 1) }.time)
            "en $mes"
        }

        val categoria = categoriaConsultada(consulta)
        if (categoria != null) {
            val categoriaNormalizada = normalizar(categoria)
            val resultados = gastos.filter { movimiento ->
                val categoriaGuardada = normalizar(movimiento.categoria)
                categoriaGuardada == categoriaNormalizada || when (categoriaNormalizada) {
                    "comida y restaurantes" -> categoriaGuardada.contains("comida") || categoriaGuardada.contains("restaurante")
                    "supermercados y mercado" -> categoriaGuardada.contains("supermercado") || categoriaGuardada == "mercado"
                    "transporte y gasolina" -> categoriaGuardada.contains("transporte") || categoriaGuardada.contains("gasolina")
                    "facturas y servicios" -> categoriaGuardada.contains("servicio") || categoriaGuardada.contains("factura")
                    "compras y comercio" -> categoriaGuardada.contains("compra") || categoriaGuardada.contains("comercio")
                    else -> false
                }
            }
            if (resultados.isEmpty()) return "No encuentro gastos de $categoria $periodo."
            val total = resultados.sumOf { it.monto }
            val detalle = if (listOf("detalle", "cuales", "que movimientos").any { consulta.contains(it) }) {
                "\n" + resultados.sortedByDescending { it.fecha }.take(6).joinToString("\n") {
                    "• ${it.titulo}: ${cop(it.monto)}"
                }
            } else ""
            return "${saludo}registras ${cop(total)} en $categoria $periodo (${resultados.size} movimientos).$detalle"
        }

        val comercio = listOf(
            "terpel", "eds urbanos", "fullgas", "texaco", "cooburquin", "uber", "didi", "rappi",
            "spotify", "amazon", "oxxo", "olimpica", "jeronimo martins", "unice", "mercadoli"
        ).firstOrNull { consulta.contains(it) }
        if (comercio != null) {
            val resultados = delPeriodo.filter { normalizar(it.titulo).contains(normalizar(comercio)) }
            if (resultados.isEmpty()) return "No encuentro movimientos de ${comercio.uppercase(Locale.ROOT)} $periodo."
            val total = resultados.sumOf { it.monto }
            val detalle = resultados.sortedByDescending { it.fecha }.take(6).joinToString("\n") {
                "• ${it.titulo}: ${cop(it.monto)}"
            }
            return "${saludo}encuentro ${resultados.size} movimientos de ${comercio.uppercase(Locale.ROOT)} por ${cop(total)} $periodo:\n$detalle"
        }

        if (listOf("suscrip", "netflix", "spotify").any { consulta.contains(it) }) {
            val suscripciones = delPeriodo.filter { movimiento ->
                listOf("netflix", "spotify", "icloud", "disney", "hbo", "youtube", "prime")
                    .any { normalizar(movimiento.titulo).contains(it) }
            }
            if (suscripciones.isEmpty()) return "No encuentro suscripciones registradas $periodo."
            return "${saludo}tus suscripciones suman ${cop(suscripciones.sumOf { it.monto })} $periodo:\n" +
                    suscripciones.take(6).joinToString("\n") { "• ${it.titulo}: ${cop(it.monto)}" }
        }

        if (listOf("ingresos", "ingreso", "recibi", "abono", "nomina").any { consulta.contains(it) }) {
            return if (ingresos.isEmpty()) "No encuentro ingresos registrados $periodo."
            else "${saludo}registras ${cop(totalIngresos)} en ingresos $periodo (${ingresos.size} movimientos)."
        }
        if (listOf("mayor gasto", "gaste mas", "categoria principal", "en que se me va").any { consulta.contains(it) }) {
            val mayor = gastosConsumo.groupBy { it.categoria }.mapValues { (_, lista) -> lista.sumOf { it.monto } }
                .maxByOrNull { it.value } ?: return "No encuentro gastos de consumo $periodo."
            val porcentaje = if (totalGastos == 0.0) 0.0 else mayor.value / totalGastos * 100
            return "${saludo}tu mayor gasto fue ${mayor.key}, con ${cop(mayor.value)} (${"%.0f".format(Locale.US, porcentaje)}% de tus gastos) $periodo."
        }
        if (listOf("balance", "ahorro", "me sobra", "me queda").any { consulta.contains(it) }) {
            if (totalIngresos == 0.0 && totalGastos == 0.0) return "No encuentro ingresos ni gastos registrados $periodo."
            val diferencia = totalIngresos - totalGastos
            return if (diferencia >= 0) {
                "${saludo}registras ${cop(totalIngresos)} en ingresos y ${cop(totalGastos)} en gastos de consumo $periodo. La diferencia es ${cop(diferencia)}."
            } else {
                "${saludo}tus gastos de consumo (${cop(totalGastos)}) superan tus ingresos (${cop(totalIngresos)}) por ${cop(-diferencia)} $periodo."
            }
        }
        if (listOf("cuanto gaste", "cuanto he gastado", "total de gastos", "mis gastos", "gastos en total").any { consulta.contains(it) } ||
            (consulta.contains("gasto") && (consulta.contains("total") || consulta.contains("cuanto")))) {
            return "${saludo}registras ${cop(totalGastos)} en gastos de consumo $periodo (${gastosConsumo.size} movimientos)."
        }
        if (listOf("movimiento", "transaccion", "extracto").any { consulta.contains(it) }) {
            val recientes = delPeriodo.sortedByDescending { it.fecha }.take(6)
            if (recientes.isEmpty()) return "No encuentro movimientos $periodo."
            return "Estos son algunos movimientos $periodo:\n" + recientes.joinToString("\n") {
                "• ${it.titulo}: ${cop(it.monto)} (${it.categoria})"
            }
        }

        return "Puedo consultar tus gastos por categoría o comercio, ingresos, balance y movimientos. Prueba: «¿Cuánto gasté en Terpel este mes?» o «¿Cuál fue mi mayor gasto en octubre?»."
    }

    private fun categoriaConsultada(consulta: String): String? = when {
        listOf("comida", "restaurante", "almuerzo").any { consulta.contains(it) } -> "Comida y Restaurantes"
        listOf("supermercado", "mercado").any { consulta.contains(it) } -> "Supermercados y Mercado"
        listOf("transporte", "gasolina", "taxi", "peaje").any { consulta.contains(it) } -> "Transporte y Gasolina"
        consulta.contains("entretenimiento") -> "Entretenimiento"
        listOf("servicio", "factura", "luz", "agua", "internet").any { consulta.contains(it) } -> "Facturas y Servicios"
        listOf("compras", "ropa").any { consulta.contains(it) } -> "Compras y Comercio"
        listOf("salud", "drogueria", "farmacia").any { consulta.contains(it) } -> "Salud y Droguerías"
        listOf("tarjeta", "credito", "cuota de manejo").any { consulta.contains(it) } -> "Pago de Tarjetas"
        listOf("transferencia", "nequi", "daviplata").any { consulta.contains(it) } -> "Transferencias y Envíos"
        consulta.contains("bolsillo") -> "Bolsillos y Ahorro"
        listOf("comision", "impuesto", "4x1000", "gmf").any { consulta.contains(it) } -> "Comisiones e Impuestos"
        else -> null
    }

    private fun cop(monto: Double): String = "$" + String.format(Locale.US, "%,.0f", monto).replace(',', '.')

    private fun normalizar(texto: String): String = Normalizer
        .normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
}
