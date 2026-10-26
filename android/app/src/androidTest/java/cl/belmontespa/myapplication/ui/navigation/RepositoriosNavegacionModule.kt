package cl.belmontespa.myapplication.ui.navigation

import cl.belmontespa.myapplication.data.local.repository.*
import cl.belmontespa.myapplication.data.repository.*
import cl.belmontespa.myapplication.di.RepositorioModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

// La prueba controla AuthRepository mediante BindValue; las otras interfaces siguen resolviéndose.
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositorioModule::class])
abstract class RepositoriosNavegacionModule {
    @Binds abstract fun cuentas(local: CuentaLocalRepository): CuentaRepository
    @Binds abstract fun viajes(local: ViajeLocalRepository): ViajeRepository
    @Binds abstract fun flota(local: FlotaLocalRepository): FlotaRepository
    @Binds abstract fun pagos(local: PagoLocalRepository): PagoRepository
    @Binds abstract fun catalogos(local: CatalogoLocalRepository): CatalogoRepository
    @Binds abstract fun abordaje(local: AbordajeLocalRepository): AbordajeRepository
}