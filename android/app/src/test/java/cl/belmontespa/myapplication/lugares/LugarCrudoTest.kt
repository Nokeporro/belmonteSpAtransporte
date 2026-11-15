package cl.belmontespa.myapplication.lugares

import cl.belmontespa.myapplication.model.Ubicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LugarCrudoTest {

    private fun componente(tipo: String, nombre: String) = ComponenteDeLugar(listOf(tipo, "political"), nombre)

    private val region = componente("administrative_area_level_1", "Región Metropolitana de Santiago")

    private fun lugar(
        nombre: String? = null,
        tipos: List<String> = emptyList(),
        componentes: List<ComponenteDeLugar> = listOf(region),
        direccionCompleta: String? = null,
    ) = LugarCrudo(nombre, tipos, direccionCompleta, componentes, -33.4372, -70.6506)

    @Test
    fun unEstablecimientoUsaSuNombreComoDireccion() {
        val aeropuerto = lugar(
            "Aeropuerto Internacional Arturo Merino Benítez",
            listOf("airport", "point_of_interest", "establishment"),
            listOf(
                componente("route", "Avenida Armando Cortínez Norte"),
                componente("street_number", "2200"),
                componente("locality", "Pudahuel"),
                region,
            ),
        )

        assertEquals(
            Ubicacion("Aeropuerto Internacional Arturo Merino Benítez", "Pudahuel", "Región Metropolitana de Santiago", -33.4372, -70.6506),
            aeropuerto.aUbicacion(),
        )
    }

    @Test
    fun unaDireccionUsaCalleYNumero() {
        val direccion = lugar(
            "Avenida Providencia 1234",
            listOf("street_address"),
            listOf(
                componente("street_number", "1234"),
                componente("route", "Avenida Providencia"),
                componente("administrative_area_level_3", "Providencia"),
                componente("locality", "Santiago"),
                region,
            ),
        )

        val ubicacion = direccion.aUbicacion()!!
        assertEquals("Avenida Providencia 1234", ubicacion.direccion)
        assertEquals("Providencia", ubicacion.comuna)
    }

    @Test
    fun unaCalleSinNumeroUsaSoloLaCalle() {
        val calle = lugar("Alameda", listOf("route"), listOf(componente("route", "Avenida Libertador Bernardo O'Higgins"), region))

        assertEquals("Avenida Libertador Bernardo O'Higgins", calle.aUbicacion()!!.direccion)
    }

    @Test
    fun sinCalleNiEstablecimientoUsaElNombreYDespuesLaDireccionCompleta() {
        assertEquals("Plaza de Armas", lugar("Plaza de Armas", listOf("neighborhood")).aUbicacion()!!.direccion)
        assertEquals("Santiago, Chile", lugar(null, direccionCompleta = "Santiago, Chile").aUbicacion()!!.direccion)
    }

    @Test
    fun sinNingunTextoNoHayUbicacion() {
        assertNull(lugar(" ", direccionCompleta = " ").aUbicacion())
    }

    @Test
    fun laComunaEsElNivelTresYSiFaltaLaLocalidad() {
        val conAmbos = lugar("Café", listOf("establishment"), listOf(
            componente("locality", "Santiago"), componente("administrative_area_level_3", "Ñuñoa"), region,
        ))
        val soloLocalidad = lugar("Café", listOf("establishment"), listOf(componente("locality", "Valdivia"), region))

        assertEquals("Ñuñoa", conAmbos.aUbicacion()!!.comuna)
        assertEquals("Valdivia", soloLocalidad.aUbicacion()!!.comuna)
    }

    @Test
    fun sinComunaOSinRegionQuedanVacias() {
        val ubicacion = lugar("Mirador", listOf("point_of_interest"), emptyList()).aUbicacion()!!

        assertEquals("", ubicacion.comuna)
        assertEquals("", ubicacion.region)
    }

    @Test
    fun laRegionEsElNivelUno() {
        assertEquals("Región Metropolitana de Santiago", lugar("Parque", listOf("park", "point_of_interest")).aUbicacion()!!.region)
    }

    @Test
    fun laDireccionSeRecortaA200Caracteres() {
        val largo = "A".repeat(250)

        assertEquals(200, lugar(largo, listOf("establishment")).aUbicacion()!!.direccion.length)
    }

    @Test
    fun conservaLasCoordenadasDelLugar() {
        val ubicacion = lugar("Parque", listOf("park")).aUbicacion()!!

        assertEquals(-33.4372, ubicacion.latitud, 0.0)
        assertEquals(-70.6506, ubicacion.longitud, 0.0)
    }
}
