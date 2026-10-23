package cl.belmontespa.myapplication.di

import android.content.Context
import cl.belmontespa.myapplication.data.navegacion.Navegador
import cl.belmontespa.myapplication.data.navegacion.NavegadorAndroid
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** La navegación externa es igual en `demo` y en `release`: depende del teléfono, no de dónde vivan los datos. */
@Module
@InstallIn(SingletonComponent::class)
object NavegacionModule {
    @Provides
    @Singleton
    fun navegador(@ApplicationContext contexto: Context): Navegador = NavegadorAndroid(contexto)
}
