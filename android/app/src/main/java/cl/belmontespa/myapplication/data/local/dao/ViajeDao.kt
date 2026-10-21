package cl.belmontespa.myapplication.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.belmontespa.myapplication.data.local.entity.PasajeroEntity
import cl.belmontespa.myapplication.data.local.entity.ViajeConPasajeros
import cl.belmontespa.myapplication.data.local.entity.ViajeEntity
import cl.belmontespa.myapplication.data.local.entity.ViajePasajeroEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ViajeDao {
    /** Inserta o actualiza en su lugar: no borra la fila, así que no dispara la cascada. */
    @Upsert
    abstract suspend fun guardar(viaje: ViajeEntity)

    @Upsert
    abstract suspend fun guardarPasajeros(pasajeros: List<PasajeroEntity>)

    @Upsert
    abstract suspend fun guardarDetalles(detalles: List<ViajePasajeroEntity>)

    @Query("DELETE FROM viaje_pasajeros WHERE folio = :folio")
    abstract suspend fun borrarDetalles(folio: String)

    @Query("DELETE FROM viajes WHERE folio = :folio")
    abstract suspend fun borrar(folio: String)

    /** Reemplaza un viaje y sus pasajeros en una sola transacción: o queda todo o nada. */
    @Transaction
    open suspend fun guardarConPasajeros(
        viaje: ViajeEntity,
        pasajeros: List<PasajeroEntity>,
        detalles: List<ViajePasajeroEntity>,
    ) {
        guardar(viaje)
        borrarDetalles(viaje.folio)
        guardarPasajeros(pasajeros)
        guardarDetalles(detalles)
    }

    @Query("SELECT * FROM viajes WHERE folio = :folio")
    abstract suspend fun obtener(folio: String): ViajeEntity?

    @Transaction
    @Query("SELECT * FROM viajes WHERE folio = :folio")
    abstract fun observar(folio: String): Flow<ViajeConPasajeros?>

    /**
     * Lista con los filtros que vengan (null = sin ese filtro). Orden de la API: con `ascendente`
     * (estados PENDIENTE, ASIGNADO o EN_CURSO) lo próximo primero; si no, lo reciente primero. El
     * empate se desata por el número del folio, en el mismo sentido.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM viajes
        WHERE (:correoSolicitante IS NULL OR correo_solicitante = :correoSolicitante)
          AND (:correoConductor IS NULL OR correo_conductor = :correoConductor)
          AND (:estado IS NULL OR estado = :estado)
          AND (:fecha IS NULL OR substr(fh_agenda, 1, 10) = :fecha)
        ORDER BY
          CASE WHEN :ascendente THEN fh_agenda END ASC,
          CASE WHEN :ascendente THEN folio_correlativo END ASC,
          fh_agenda DESC,
          folio_correlativo DESC
        """,
    )
    abstract fun observarLista(
        correoSolicitante: String?,
        correoConductor: String?,
        estado: String?,
        fecha: String?,
        ascendente: Boolean,
    ): Flow<List<ViajeConPasajeros>>

    /**
     * Agenda del Conductor para `fecha` (AAAA-MM-DD): sus viajes ASIGNADO, EN_CURSO y FINALIZADO de
     * ese día, por hora. Con `incluirEnCursoAnterior` (la agenda de hoy) suma su viaje EN_CURSO de
     * una fecha anterior, para que pueda finalizarlo.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM viajes
        WHERE correo_conductor = :correo
          AND ((estado IN ('ASIGNADO', 'EN_CURSO', 'FINALIZADO') AND substr(fh_agenda, 1, 10) = :fecha)
               OR (:incluirEnCursoAnterior AND estado = 'EN_CURSO' AND substr(fh_agenda, 1, 10) < :fecha))
        ORDER BY fh_agenda ASC, folio_correlativo ASC
        """,
    )
    abstract fun observarAgendaConductor(
        correo: String,
        fecha: String,
        incluirEnCursoAnterior: Boolean,
    ): Flow<List<ViajeConPasajeros>>
}
