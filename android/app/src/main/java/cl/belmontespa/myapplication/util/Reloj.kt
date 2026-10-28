package cl.belmontespa.myapplication.util

import java.time.Clock
import java.time.ZoneId

object Reloj {
    val santiago: Clock = Clock.system(ZoneId.of("America/Santiago"))
}
