package com.aya.doank.core

data class SpoofTarget(
    val id: String,
    val label: String,
    val packageNames: Set<String>
)

object Targets {
    // v2.7.1: khusus DRIVER APPS — play/stop/push hanya menyentuh package ini.
    // Customer apps (com.grabtaxi.passenger / com.gojek.app) DIKELUARKAN.
    val GRAB = SpoofTarget("grab-driver", "GRAB", setOf("com.grabtaxi.driver2"))
    val GOJEK = SpoofTarget("gojek-driver", "GOJEK", setOf("com.gojek.partner"))

    val all = listOf(GRAB, GOJEK)
    fun byId(id: String): SpoofTarget = all.first { it.id == id }
}
