package cl.belmontespa.myapplication.ui.solicitante

import cl.belmontespa.myapplication.ui.pantallas.DependenciasPantallas
import cl.belmontespa.myapplication.ui.pantallas.PantallaBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Instancia por entrada de navegación: los formularios sobreviven a la rotación.
@HiltViewModel
class SolicitanteViewModel @Inject constructor(repos: DependenciasPantallas) : PantallaBaseViewModel(repos)
