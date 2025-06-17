package com.teddeh

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.nio.file.*
import java.util.regex.Pattern

class LiveWire : JavaPlugin() {
    private var watchTask: BukkitTask? = null
    private var watchService: WatchService? = null

    /** This runs when the server enables the plugin */
    override fun onEnable() {
        // Generate the default config if it doesn't exist.
        saveDefaultConfig()

        // Search and cache a reference to all plugins.
        findAllPlugins()

        // Initiate the livewire directory watcher
        this.watchDirectory()
    }

    /** This runs when the server disables the plugin */
    override fun onDisable() {
        // Cancel the async task
        watchTask?.cancel()
        watchTask = null

        // Close the WatchService
        watchService?.close()
        watchService = null
    }

    /** Check if the plugin should be ignored for hot-reloading */
    private fun isIgnored(plugin: String): Boolean {
        val ignoreList = config.getStringList("ignore")
        for (ignore in ignoreList) {
            if (Pattern.compile(ignore).matcher(plugin).find()) {
                return true
            }
        }
        return false
    }

    /** Listen for updates within the livewire directory */
    private fun watchDirectory() {
        val root = Bukkit.getPluginsFolder().parentFile
        val liveWireDir = File(root, "livewire")

        // Create the livewire directory if it doesn't exist
        if (!liveWireDir.exists()) {
            liveWireDir.mkdir()
        }

        // Create a watch service
        watchService = FileSystems.getDefault().newWatchService()
        liveWireDir.toPath().register(
            watchService ?: return,
            StandardWatchEventKinds.ENTRY_CREATE
        )

        // Check for updates every 1 second.
        watchTask = server.scheduler.runTaskTimerAsynchronously(this, Runnable {
            try {
                val key = watchService?.poll() ?: return@Runnable
                for (event in key.pollEvents()) {

                    // If the change is not a newly added file, ignore it.
                    val kind = event.kind()
                    if (kind != StandardWatchEventKinds.ENTRY_CREATE)
                        continue

                    val fileName = event.context() as Path

                    // Ensure to only take action on jar files
                    if (!fileName.toString().endsWith(".jar"))
                        return@Runnable

                    val newPluginJar = File(liveWireDir, fileName.toString())
                    val name = getPluginNameFromJar(newPluginJar)
                    if (name == null) {
                        println("$fileName does not contain a plugin.yml")
                        return@Runnable
                    }

                    val plugin = PLUGINS.find { it.name.equals(name, true) }

                    // This is the jar being replaced, if exists.
                    val oldPluginJar = plugin?.absolutePath?.let { File(it) }

                    val ignored = isIgnored(name)
                    if (ignored) println("$name is ignoring hot-reloads")

                    // Ensure this part runs on bukkit's main thread
                    server.scheduler.runTask(this, Runnable {
                        var output: Path? = null

                        if (!ignored) {
                            // If the plugin currently exists, destroy it!
                            if (plugin != null) disablePlugin(plugin)

                            // Delete the plugin jar in the plugins directory.
                            if (oldPluginJar != null) {
                                Files.deleteIfExists(oldPluginJar.toPath())
                            }

                            // Copy over the new plugin jar from livewire directory.
                            output = Files.copy(
                                newPluginJar.toPath(),
                                File(Bukkit.getPluginsFolder(), fileName.toString()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING
                            )
                        }

                        // Delete the plugin from livewire directory
                        Files.deleteIfExists(newPluginJar.toPath())

                        if (!ignored) {
                            // Load the plugin
                            enablePlugin(output?.toFile()?.absolutePath ?: return@Runnable)

                            // Refresh plugin references
                            findAllPlugins()

                            // Also reload plugins that depend on THIS plugin.
                            reloadIfDependsOn(name)
                        }
                    })
                }
                key.reset()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 20L, 20L)
    }

    /** Reload plugins that rely on x */
    private fun reloadIfDependsOn(name: String) {
        val plugin = PLUGINS.find { it.name.equals(name, true) }
        if (plugin == null) {
            println("Unable to find plugin $name")
            return
        }

        for (reliant in plugin.reliants) {
            if (reliant.name == name) continue
            if (isIgnored(reliant.name)) continue

            disablePlugin(reliant)
            enablePlugin(reliant.absolutePath, false)
        }
    }
}
