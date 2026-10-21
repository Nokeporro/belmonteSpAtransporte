package cl.belmontespa.myapplication.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.ZoneId
import javax.inject.Singleton

/** "Ahora" sale de este `Clock` en hora de Chile, nunca de la zona del sistema; las pruebas lo fijan. */
@Module
@InstallIn(SingletonComponent::class)
object RelojModule {
    @Provides
    @Singleton
    fun reloj(): Clock = Clock.system(ZoneId.of("America/Santiago"))
}
