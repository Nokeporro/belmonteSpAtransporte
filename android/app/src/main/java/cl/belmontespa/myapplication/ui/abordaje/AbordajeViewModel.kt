package cl.belmontespa.myapplication.ui.abordaje

import cl.belmontespa.myapplication.ui.pantallas.DependenciasPantallas
import cl.belmontespa.myapplication.ui.pantallas.PantallaBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Instancia por entrada de navegación: los formularios sobreviven a la rotación.
@HiltViewModel
class AbordajeViewModel @Inject constructor(repos: DependenciasPantallas) : PantallaBaseViewModel(repos)
