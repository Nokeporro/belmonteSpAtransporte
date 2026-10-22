package cl.belmontespa.myapplication

import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Reloj que se puede mover, para recalcular el retraso o el día sin esperar. */
internal class RelojMovil(var ahora: LocalDateTime, private val zona: ZoneId) : Clock() {
    override fun getZone(): ZoneId = zona
    override fun withZone(zone: ZoneId?): Clock = this
    override fun instant(): Instant = ahora.atZone(zona).toInstant()
}
