package com.teddeh.ext

import org.bukkit.plugin.java.JavaPlugin

@Suppress("UnstableApiUsage")
fun JavaPlugin.dependsOn(plugin: String): Boolean {
    return this.pluginMeta.pluginDependencies.contains(plugin)
            || this.pluginMeta.pluginSoftDependencies.contains(plugin)
}