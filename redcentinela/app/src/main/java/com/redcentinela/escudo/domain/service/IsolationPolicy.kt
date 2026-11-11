package com.redcentinela.escudo.domain.service

import com.redcentinela.escudo.domain.model.ShieldStatus

/**
 * Qué hace el equipo cuando se queda solo, sin que nadie pueda avisarle nada.
 * Dos decisiones, las dos pensadas para maximizar la recuperación:
 *
 * 1. **Alerta sola.** Una orden de "robado" solo llega por internet, SMS o
 *    con alguien del círculo a metros. Si el equipo está bloqueado, sin
 *    ningún canal y lejos de su guardián durante un rato, nadie podría
 *    avisarle: entra en alerta por su cuenta.
 *
 * 2. **Apagado falso primero.** Mientras el equipo siga comunicándose,
 *    conviene que quien lo tiene crea que está apagado: no lo abre, no lo
 *    resetea, y la ubicación sigue saliendo. La pantalla de "celular
 *    perdido" se guarda. Solo cuando ya no queda ningún canal (ni un
 *    Bluetooth del círculo cerca) el disimulo deja de servir y vale más
 *    mostrarle a quien lo tenga cómo devolverlo.
 *
 * Sin estado propio ni Android: recibe los hechos y devuelve la decisión.
 */
object IsolationPolicy {

    class Facts(
        val status: ShieldStatus,
        val locked: Boolean,
        /** Hay internet o SMS ahora mismo. */
        val reachable: Boolean,
        val hasGuardian: Boolean,
        val guardianNear: Boolean,
        /** Desde cuándo se cumple "bloqueado, sin canal y sin guardián" (null = no se cumple). */
        val aloneSince: Long?,
        /** Último contacto real con el círculo: envío entregado, orden recibida o Bluetooth cercano. */
        val lastContactAt: Long,
        val alertSince: Long?,
        val fakeShutdownActive: Boolean,
        /** La pantalla de perdido ya se mostró en esta alerta por falta de canales. */
        val revealed: Boolean
    )

    /** ¿Se cumple ahora la condición de aislamiento que arranca el contador de la alerta sola? */
    fun alone(facts: Facts): Boolean =
        facts.status == ShieldStatus.ARMADO && facts.locked && !facts.reachable &&
            facts.hasGuardian && !facts.guardianNear

    fun shouldSelfAlert(facts: Facts, now: Long, afterMillis: Long): Boolean =
        afterMillis > 0 && alone(facts) && facts.aloneSince != null && now - facts.aloneSince >= afterMillis

    /** En alerta: ¿ya no queda ningún canal desde hace [afterMillis]? */
    fun cutOff(facts: Facts, now: Long, afterMillis: Long): Boolean {
        if (!facts.status.isAlert || facts.reachable) return false
        val since = maxOf(facts.lastContactAt, facts.alertSince ?: 0L)
        return now - since >= afterMillis
    }

    enum class Screen { NORMAL, APAGADO_FALSO, PERDIDO }

    /** Qué debe verse en la pantalla del equipo. */
    fun screen(facts: Facts, now: Long, revealAfterMillis: Long): Screen = when {
        !facts.status.isAlert -> Screen.NORMAL
        facts.revealed -> Screen.PERDIDO
        facts.fakeShutdownActive ->
            if (cutOff(facts, now, revealAfterMillis)) Screen.PERDIDO else Screen.APAGADO_FALSO
        facts.status == ShieldStatus.ALERTA_PERDIDA -> Screen.PERDIDO
        else -> Screen.NORMAL
    }
}
