package cl.belmontespa.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Punto de entrada de Hilt: crea el grafo de dependencias de toda la app. */
@HiltAndroidApp
class BelmonteApplication : Application()
