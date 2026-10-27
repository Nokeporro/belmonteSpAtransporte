package cl.belmontespa.myapplication.viewmodel

import android.app.Application
import cl.belmontespa.myapplication.repository.SoporteLocal

/** Un `Application` que nunca se usa: los ViewModel de las pruebas reciben sus repositorios por constructor. */
internal fun aplicacionFalsa(): Application = instanciarSinConstructor(Application::class.java)

/** Un `SoporteLocal` vacío para subclasificar un Repository: el doble sobrescribe todo lo que la prueba usa. */
internal fun soporteVacio(): SoporteLocal = instanciarSinConstructor(SoporteLocal::class.java)

private fun <T> instanciarSinConstructor(clase: Class<T>): T {
    val campo = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe").apply { isAccessible = true }
    val unsafe = campo.get(null)
    @Suppress("UNCHECKED_CAST")
    return unsafe.javaClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, clase) as T
}
