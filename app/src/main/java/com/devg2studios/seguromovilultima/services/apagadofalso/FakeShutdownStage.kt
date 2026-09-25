package com.devg2studios.seguromovilultima.services.apagadofalso

enum class ShutdownStage {
    MENU,        // Menú falso mostrando opciones
    POWERING_OFF, // Animación de "Apagando..."
    OFF          // Pantalla negra final
}

// Objeto compartido para que el estado sobreviva si el sistema recrea la Activity
// (por ejemplo, al apagar/encender la pantalla real varias veces)
object FakeShutdownStageHolder {
    @Volatile
    var current: ShutdownStage = ShutdownStage.MENU
}