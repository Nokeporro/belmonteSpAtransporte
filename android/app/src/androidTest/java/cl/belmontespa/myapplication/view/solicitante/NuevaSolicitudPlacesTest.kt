package cl.belmontespa.myapplication.view.solicitante

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import cl.belmontespa.myapplication.EntornoPruebas
import cl.belmontespa.myapplication.MainActivity
import cl.belmontespa.myapplication.R
import cl.belmontespa.myapplication.lugares.ComponenteDeLugar
import cl.belmontespa.myapplication.lugares.LugarCrudo
import cl.belmontespa.myapplication.lugares.ResultadoDeBusqueda
import cl.belmontespa.myapplication.lugares.ServicioDeLugares
import cl.belmontespa.myapplication.model.ErrorApp
import cl.belmontespa.myapplication.model.Resultado
import cl.belmontespa.myapplication.model.Rol
import cl.belmontespa.myapplication.model.Sesion
import cl.belmontespa.myapplication.repository.viaje
import cl.belmontespa.myapplication.ui.theme.BelmonteTheme
import cl.belmontespa.myapplication.viewmodel.solicitante.NuevaSolicitudViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NuevaSolicitudPlacesTest {
    @get:Rule(order = 0) val entorno = EntornoPruebas()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val servicio = ServicioDePrueba()
    private var creada: String? = null

    @Before fun preparar() {
        runBlocking {
            entorno.sesiones.guardar(
                Sesion("prueba", "crios@andesconsult.cl", Rol.SOLICITANTE, entorno.local.clock.millis() + 3_600_000),
            )
        }
    }

    private fun texto(id: Int) = compose.activity.getString(id)
    private fun campo(id: Int) = compose.onNode(hasText(texto(id)) and hasSetTextAction())
    private fun accion(id: Int) = compose.onNode(hasText(texto(id), substring = true) and hasClickAction())
    private fun opcion(nombre: String) = compose.onNode(hasText(nombre) and hasClickAction())
    private fun esperarTexto(texto: String) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty() }
    private fun esperarSinTexto(texto: String) =
        compose.waitUntil(15_000) { compose.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isEmpty() }

    private fun abrir() {
        val registro = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent())
            }
        }
        val dueno = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registro
        }
        val viewModel = NuevaSolicitudViewModel(compose.activity.application, servicio = servicio)
        compose.activity.runOnUiThread {
            compose.activity.setContent {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides dueno) {
                    BelmonteTheme {
                        NuevaSolicitudScreen(onNavegar = { _, folio -> creada = folio }, onVolver = {}, viewModel = viewModel)
                    }
                }
            }
        }
        esperarTexto(texto(R.string.campo_origen))
    }

    private fun buscar(id: Int, placeId: String) {
        servicio.siguiente = ResultadoDeBusqueda.Elegido(placeId)
        accion(id).performScrollTo().performClick()
    }

    private fun agregarPasajero() {
        campo(R.string.campo_nombre).performScrollTo().performTextReplacement("Invitada Prueba")
        campo(R.string.campo_documento).performScrollTo().performTextReplacement("12345678-5")
        accion(R.string.accion_agregar_pasajero).performScrollTo().performClick()
    }

    @Test fun cadaCampoAbreLaBusquedaYMuestraElLugarElegido() {
        abrir()
        esperarTexto(texto(R.string.aviso_busca_lugar))

        buscar(R.string.campo_origen, "aeropuerto")
        esperarTexto("Aeropuerto Arturo Merino Benítez")
        buscar(R.string.campo_destino, "providencia")
        esperarTexto("Avenida Providencia 1234")
    }

    @Test fun unaBusquedaFallidaAvisaYNoCambiaElCampo() {
        abrir()

        servicio.siguiente = ResultadoDeBusqueda.Fallido
        accion(R.string.campo_origen).performScrollTo().performClick()

        esperarTexto(texto(R.string.aviso_lugar_no_disponible))
        assertEquals(2, compose.onAllNodesWithText(texto(R.string.aviso_busca_lugar)).fetchSemanticsNodes().size)
    }

    @Test fun unaComunaFueraDelCatalogoSeCorrigeConElSelectorYLaSolicitudSeCrea() {
        abrir()
        buscar(R.string.campo_origen, "aeropuerto")
        esperarTexto("Aeropuerto Arturo Merino Benítez")
        buscar(R.string.campo_destino, "inventada")
        esperarTexto("Calle Inventada 99")
        agregarPasajero()
        esperarTexto("Invitada Prueba")

        accion(R.string.accion_guardar).performScrollTo().performClick()
        esperarTexto(texto(R.string.aviso_comuna_faltante))

        accion(R.string.campo_region).performScrollTo().performClick()
        opcion("Metropolitana de Santiago").performClick()
        accion(R.string.campo_comuna).performScrollTo().performClick()
        opcion("Las Condes").performScrollTo().performClick()
        esperarSinTexto(texto(R.string.aviso_comuna_faltante))

        accion(R.string.accion_guardar).performScrollTo().performClick()
        compose.waitUntil(15_000) { creada != null }
        val viaje = runBlocking { entorno.local.viaje(checkNotNull(creada)) }
        assertEquals("Aeropuerto Arturo Merino Benítez", viaje.origenDireccion)
        assertEquals("Pudahuel", viaje.origenComuna)
        assertEquals("Calle Inventada 99", viaje.destinoDireccion)
        assertEquals("Las Condes", viaje.destinoComuna)
        assertEquals("Metropolitana de Santiago", viaje.destinoRegion)
        assertEquals(-33.4, viaje.destinoLatitud, 0.0)
    }
}

private class ServicioDePrueba : ServicioDeLugares {
    override val disponible = true
    var siguiente: ResultadoDeBusqueda = ResultadoDeBusqueda.Cancelado

    private fun lugar(nombre: String, tipos: List<String>, comuna: String, latitud: Double, longitud: Double) = LugarCrudo(
        nombre, tipos, null,
        listOf(
            ComponenteDeLugar(listOf("administrative_area_level_3"), comuna),
            ComponenteDeLugar(listOf("administrative_area_level_1"), "Región Metropolitana de Santiago"),
        ),
        latitud, longitud,
    )

    private val lugares = mapOf(
        "aeropuerto" to lugar("Aeropuerto Arturo Merino Benítez", listOf("airport", "establishment"), "Pudahuel", -33.393, -70.7858),
        "providencia" to lugar("Avenida Providencia 1234", listOf("street_address"), "Providencia", -33.4256, -70.6166),
        "inventada" to lugar("Calle Inventada 99", listOf("street_address"), "Comuna Inventada", -33.4, -70.6),
    )

    override fun intentDeBusqueda(contexto: Context): Intent = Intent()

    override fun resultadoDeBusqueda(codigo: Int, datos: Intent?): ResultadoDeBusqueda = siguiente

    override suspend fun obtener(placeId: String): Resultado<LugarCrudo> =
        lugares[placeId]?.let { Resultado.Exito(it) } ?: Resultado.Fallo(ErrorApp.NoEncontrado)
}
