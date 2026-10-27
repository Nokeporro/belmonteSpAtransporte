package cl.belmontespa.myapplication.di

import cl.belmontespa.myapplication.data.local.DatosDemo
import cl.belmontespa.myapplication.model.Ubicacion
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

/**
 * Lugares que «Nueva solicitud» ofrece como origen y destino. Mientras Google Places no esté integrado (Etapa 2) son
 * los lugares ficticios de la semilla; en la Etapa 2 este provider se reemplaza por la búsqueda de Places.
 */
@Module
@InstallIn(SingletonComponent::class)
object LugaresModule {
    @Provides
    @Named("lugares_demo")
    fun lugares(): List<Ubicacion> = DatosDemo.lugares
}
