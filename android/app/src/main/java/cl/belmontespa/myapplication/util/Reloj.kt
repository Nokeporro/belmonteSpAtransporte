package cl.belmontespa.myapplication.util

import java.time.Clock
import java.time.ZoneId

/** "Ahora" sale de este `Clock` en hora de Chile, nunca de la zona del sistema; las pruebas lo fijan. */
object Reloj {
    val santiago: Clock = Clock.system(ZoneId.of("America/Santiago"))
}
