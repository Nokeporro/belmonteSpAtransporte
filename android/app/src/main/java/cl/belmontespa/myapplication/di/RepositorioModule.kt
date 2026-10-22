package cl.belmontespa.myapplication.di

import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.data.local.repository.AbordajeLocalRepository
import cl.belmontespa.myapplication.data.local.repository.AuthLocalRepository
import cl.belmontespa.myapplication.data.local.repository.CatalogoLocalRepository
import cl.belmontespa.myapplication.data.local.repository.CuentaLocalRepository
import cl.belmontespa.myapplication.data.local.repository.FlotaLocalRepository
import cl.belmontespa.myapplication.data.local.repository.PagoLocalRepository
import cl.belmontespa.myapplication.data.local.repository.ViajeLocalRepository
import cl.belmontespa.myapplication.data.remote.repository.AbordajeRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.AuthRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.CatalogoRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.CuentaRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.FlotaRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.PagoRemoteRepository
import cl.belmontespa.myapplication.data.remote.repository.ViajeRemoteRepository
import cl.belmontespa.myapplication.data.repository.AbordajeRepository
import cl.belmontespa.myapplication.data.repository.AuthRepository
import cl.belmontespa.myapplication.data.repository.CatalogoRepository
import cl.belmontespa.myapplication.data.repository.CuentaRepository
import cl.belmontespa.myapplication.data.repository.FlotaRepository
import cl.belmontespa.myapplication.data.repository.PagoRepository
import cl.belmontespa.myapplication.data.repository.ViajeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Elige, para cada repositorio, la implementación local (Room, build demo) o la remota (backend),
 * según `BuildConfig.MODO_LOCAL`. Los ViewModel solo conocen las interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositorioModule {
    // La UI recibe valores del modelo; no accede a la implementación de DatosDemo.
    @Provides
    @javax.inject.Named("ubicaciones_demo")
    fun ubicacionesDemo(): List<cl.belmontespa.myapplication.model.Ubicacion> =
        if (BuildConfig.MODO_LOCAL) cl.belmontespa.myapplication.data.local.DatosDemo.ubicaciones else emptyList()

    @Provides
    @Singleton
    fun auth(local: Provider<AuthLocalRepository>, remota: Provider<AuthRemoteRepository>): AuthRepository =
        if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun cuenta(local: Provider<CuentaLocalRepository>, remota: Provider<CuentaRemoteRepository>): CuentaRepository =
        if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun catalogo(
        local: Provider<CatalogoLocalRepository>,
        remota: Provider<CatalogoRemoteRepository>,
    ): CatalogoRepository = if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun viaje(local: Provider<ViajeLocalRepository>, remota: Provider<ViajeRemoteRepository>): ViajeRepository =
        if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun flota(local: Provider<FlotaLocalRepository>, remota: Provider<FlotaRemoteRepository>): FlotaRepository =
        if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun pago(local: Provider<PagoLocalRepository>, remota: Provider<PagoRemoteRepository>): PagoRepository =
        if (BuildConfig.MODO_LOCAL) local.get() else remota.get()

    @Provides
    @Singleton
    fun abordaje(
        local: Provider<AbordajeLocalRepository>,
        remota: Provider<AbordajeRemoteRepository>,
    ): AbordajeRepository = if (BuildConfig.MODO_LOCAL) local.get() else remota.get()
}
