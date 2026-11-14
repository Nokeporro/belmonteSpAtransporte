package cl.belmontespa.myapplication.e2e

import android.content.Context
import android.net.ConnectivityManager
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.database.AppDatabase
import cl.belmontespa.myapplication.session.SesionStore
import cl.belmontespa.myapplication.util.Reloj
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.rules.ExternalResource

const val CLAVE_DEMO = "Belmonte2026"

class EntornoE2E : ExternalResource() {
    private val contexto: Context = ApplicationProvider.getApplicationContext()
    val api = ApiE2E()

    override fun before() {
        assumeTrue(
            "FlujosE2ETest necesita un backend con database/demo.sql: pasar -e e2e true a am instrument",
            InstrumentationRegistry.getArguments().getString("e2e") == "true",
        )
        limpiarLaApp()
    }

    override fun after() {
        cambiarRed(true)
        limpiarLaApp()
    }

    fun cortarRed() {
        cambiarRed(false)
        esperarRed(false)
    }

    fun restaurarRed() {
        cambiarRed(true)
        esperarRed(true)
    }

    private fun cambiarRed(activa: Boolean) {
        val accion = if (activa) "enable" else "disable"
        for (servicio in listOf("wifi", "data")) shell("svc $servicio $accion")
    }

    private fun esperarRed(conRed: Boolean) {
        val conectividad = contexto.getSystemService(ConnectivityManager::class.java)
        val limite = System.currentTimeMillis() + 30_000
        while ((conectividad.activeNetwork != null) != conRed) {
            if (System.currentTimeMillis() > limite) fail("La red no quedó ${if (conRed) "activa" else "cortada"} en 30 s")
            Thread.sleep(250)
        }
    }

    private fun shell(comando: String) {
        val automatizacion = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automatizacion.executeShellCommand(comando)).use { it.readBytes() }
    }

    private fun limpiarLaApp() {
        WorkManager.getInstance(contexto).cancelAllWork().result.get()
        runBlocking { SesionStore.getInstance(contexto).borrar() }
        AppDatabase.getDatabase(contexto).clearAllTables()
    }

    fun exigirBackend() {
        try {
            api.viajes()
        } catch (e: Exception) {
            fail("No se pudo consultar el backend en ${BuildConfig.API_BASE_URL} con database/demo.sql cargado: ${e.message}")
        }
    }

    fun exigirPendiente(folio: String) {
        exigirBackend()
        if (api.viaje(folio).getString("estado") != "PENDIENTE") fail("$folio no está PENDIENTE. $RECARGA")
    }

    fun exigirEnVentanaDeInicio(folio: String) {
        exigirBackend()
        val viaje = api.viaje(folio)
        if (viaje.getString("estado") != "ASIGNADO") fail("$folio no está ASIGNADO. $RECARGA")
        val agenda = LocalDateTime.parse(viaje.getString("fhAgenda"))
        if (LocalDateTime.now(Reloj.santiago).isAfter(agenda.plus(MARGEN_DE_VENTANA))) fail("$folio está por salir de su ventana de inicio. $RECARGA")
    }

    private companion object {
        val MARGEN_DE_VENTANA: Duration = Duration.ofMinutes(45)
        const val RECARGA = "Recarga database/demo.sql antes de cada corrida y corre dentro de los 90 min siguientes (ventana de inicio de TR-1005)."
    }
}

class ApiE2E {
    private val base = BuildConfig.API_BASE_URL + "api/v1"
    private val tokens = mutableMapOf<String, String>()

    fun viaje(folio: String): JSONObject = JSONObject(obtener("/viajes/$folio"))

    fun viajes(): List<JSONObject> {
        val lista = JSONArray(obtener("/viajes"))
        return List(lista.length()) { lista.getJSONObject(it) }
    }

    private fun obtener(ruta: String): String {
        val conexion = URL(base + ruta).openConnection() as HttpURLConnection
        conexion.setRequestProperty("Authorization", "Bearer " + token(ADMINISTRADOR))
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 10_000
        try {
            if (conexion.responseCode != 200) fail("GET $ruta respondió ${conexion.responseCode}")
            return conexion.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conexion.disconnect()
        }
    }

    private fun token(correo: String): String = tokens.getOrPut(correo) {
        val conexion = URL("$base/auth/login").openConnection() as HttpURLConnection
        conexion.requestMethod = "POST"
        conexion.doOutput = true
        conexion.setRequestProperty("Content-Type", "application/json")
        conexion.connectTimeout = 10_000
        conexion.readTimeout = 10_000
        try {
            conexion.outputStream.use { it.write(JSONObject().put("correo", correo).put("password", CLAVE_DEMO).toString().toByteArray()) }
            if (conexion.responseCode != 200) fail("Login de $correo respondió ${conexion.responseCode}")
            JSONObject(conexion.inputStream.bufferedReader().use { it.readText() }).getString("accessToken")
        } finally {
            conexion.disconnect()
        }
    }

    private companion object {
        const val ADMINISTRADOR = "recepcion@belmontespa.cl"
    }
}
