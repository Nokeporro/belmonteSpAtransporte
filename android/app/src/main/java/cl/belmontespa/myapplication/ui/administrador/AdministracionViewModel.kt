package cl.belmontespa.myapplication.ui.administrador

import cl.belmontespa.myapplication.ui.pantallas.DependenciasPantallas
import cl.belmontespa.myapplication.ui.pantallas.PantallaBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Instancia por entrada de navegación: los formularios sobreviven a la rotación.
@HiltViewModel
class AdministracionViewModel @Inject constructor(repos: DependenciasPantallas) : PantallaBaseViewModel(repos)
