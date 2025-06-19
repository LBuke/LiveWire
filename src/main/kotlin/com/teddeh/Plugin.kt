@file:Suppress("UnstableApiUsage")

package com.teddeh

import com.teddeh.ext.dependsOn
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile

val PLUGINS = mutableSetOf<Plugin>()

class Plugin(
    var javaPlugin: WeakReference<JavaPlugin>,
    val name: String,
    private var version: String,
    private val fileName: String,
    val absolutePath: String,
    var reliants: MutableList<Plugin> = mutableListOf(),
) {
    var classLoader: WeakReference<ClassLoader?>? = null

    init {
        val plugin = PLUGINS.find { it.name.equals(this.name, true) }
        if (plugin == null) {
            PLUGINS.add(this)
        } else {
            plugin.javaPlugin = this.javaPlugin
            plugin.version = this.version
        }
    }

    /** Search for plugins that depend on THIS plugin */
    fun resolveReliants() {
        reliants.clear()
        reliants.addAll(PLUGINS.filter { it.javaPlugin.get()?.dependsOn(name) ?: false })
    }

    /** Pretty print the plugin */
    override fun toString(): String {
        return buildString {
            appendLine("Plugin(name='$name', version='$version')")
            appendLine("  File: $fileName")
            appendLine("  Path: $absolutePath")
            appendLine("  Reliants: ${reliants.joinToString { it.name }}")
        }
    }
}

/** Scan all plugins and store a cached reference of them */
fun findAllPlugins() {
    val classLoaderField = JavaPlugin::class.java.getDeclaredField("classLoader")
    classLoaderField.isAccessible = true

    Bukkit.getPluginsFolder().listFiles()
        ?.filter { it.name.endsWith(".jar") }
        ?.forEach { file ->
            runCatching {
                JarFile(file).use { jar ->
                    val entry = jar.getEntry("plugin.yml")
                        ?: jar.getEntry("paper-plugin.yml")
                        ?: return@use

                    jar.getInputStream(entry).bufferedReader().use { reader ->
                        val name = YamlConfiguration.loadConfiguration(reader).getString("name")
                        if (name == null) {
                            println("Could not resolve ${file.name}")
                            return@use
                        }

                        val plugin = Bukkit.getPluginManager().getPlugin(name) as JavaPlugin
                        Plugin(
                            WeakReference(plugin),
                            plugin.pluginMeta.name,
                            plugin.pluginMeta.version,
                            file.name,
                            file.absolutePath,
                        )
                    }
                }
            }.onFailure {
                println("Failed to read ${file.name}: ${it.message}")
            }
        }

    PLUGINS.forEach {
        it.classLoader = WeakReference(classLoaderField.get(it.javaPlugin.get()) as? ClassLoader)
        it.resolveReliants()
    }
}

private const val MAX_RETRIES = 10
private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

/** Get the plugin name from the jar async, tries multiple times */
fun getPluginNameFromJarAsync(
    jar: File,
    attempt: Int = 0
): CompletableFuture<String> {
    // Stop retrying after MAX_RETRIES
    if (attempt >= MAX_RETRIES) {
        return CompletableFuture.completedFuture(null)
    }

    val promise = CompletableFuture<String>()

    // Try immediately
    scheduler.execute {
        try {
            JarFile(jar).use { jarFile ->
                val entry = jarFile.getEntry("plugin.yml")
                    ?: jarFile.getEntry("paper-plugin.yml")

                if (entry != null) {
                    jarFile.getInputStream(entry).bufferedReader().use { reader ->
                        val name = YamlConfiguration
                            .loadConfiguration(reader)
                            .getString("name")

                        if (name != null) {
                            promise.complete(name)
                        } else {
                            promise.complete(null)  // no name field
                        }
                    }
                } else {
                    promise.complete(null)      // no plugin.yml
                }
            }
        } catch (_: Exception) {
            // schedule a retry in 1 second
            scheduler.schedule({
                getPluginNameFromJarAsync(jar, attempt + 1)
                    .whenComplete { result, _ ->
                        promise.complete(result)
                    }
            }, 2, TimeUnit.SECONDS)
        }
    }

    return promise
}

