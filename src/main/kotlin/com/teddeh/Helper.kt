package com.teddeh

import com.teddeh.ext.comp
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.PluginCommand
import org.bukkit.command.SimpleCommandMap
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.PluginClassLoader
import java.io.File
import java.util.*
import java.util.jar.JarFile

@Suppress("UnstableApiUsage")
fun enablePlugin(path: String, notify: Boolean = true) {
    val plugin = Bukkit.getPluginManager().loadPlugin(File(path))
    if (plugin != null) {
        Bukkit.getPluginManager().enablePlugin(plugin)

        if (!notify) return

        val name = plugin.pluginMeta.name
        val ver = plugin.pluginMeta.version

        val text = "<white>LiveWire » <gray><i>Hot swapping %s (%s)".format(name, ver).comp()
        Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(text)
    }
}

@Suppress("UnstableApiUsage")
fun disablePlugin(plugin: Plugin) {
    val javaPlugin = plugin.javaPlugin.get()
    if (javaPlugin == null) {
        println("JavaPlugin is not available for plugin: ${plugin.name}")
        return
    }

    val pluginManager = Bukkit.getPluginManager()

    // If the plugin is enabled, disable it.
    if (javaPlugin.isEnabled) {
        pluginManager.disablePlugin(javaPlugin)
    }

    // Unregister all known listeners
    HandlerList.unregisterAll(javaPlugin)

    // Cancel all known running tasks
    Bukkit.getScheduler().cancelTasks(javaPlugin)

    // Unregister all known commands
    removeCommands(javaPlugin)

    val classLoader = plugin.classLoader?.get()
    if (classLoader == null) {
        println("ClassLoader is not available for plugin: ${plugin.name}")
        return
    }

    // Close the class loader & jar reference
    if (classLoader is PluginClassLoader) {
        val jarField = PluginClassLoader::class.java.getDeclaredField("jar")
        jarField.isAccessible = true
        val jar = jarField.get(classLoader) as JarFile
        jar.close()
        classLoader.close()
    }

    removeFromPluginsList(javaPlugin)
    removeFromLookupNames(javaPlugin)

    System.gc()
}

fun removeCommands(plugin: JavaPlugin) {
    val pluginManager = Bukkit.getServer().pluginManager
    val commandMapField = pluginManager.javaClass.getDeclaredField("commandMap").apply { isAccessible = true }
    val commandMap = commandMapField.get(pluginManager) as SimpleCommandMap
    val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val knownCommands = knownCommandsField.get(commandMap) as HashMap<String, Command>
    val toRemove = knownCommands.entries.filter { (_, cmd) ->
        cmd is PluginCommand && Objects.equals(cmd.plugin, plugin)
    }.map { it.key }
    toRemove.forEach { key -> knownCommands.remove(key) }
}

fun removeFromPluginsList(plugin: JavaPlugin) {
    val instance = PaperPluginManagerImpl.getInstance()
    val instanceManager = instance.javaClass.getDeclaredField("instanceManager").apply { isAccessible = true }.get(instance)

    instanceManager.javaClass.getDeclaredField("plugins").apply { isAccessible = true }.get(instanceManager).let {
        @Suppress("UNCHECKED_CAST")
        val plugins = it as ArrayList<org.bukkit.plugin.Plugin>
        plugins.remove(plugin)
    }
}

fun removeFromLookupNames(plugin: JavaPlugin) {
    val instance = PaperPluginManagerImpl.getInstance()
    val instanceManager = instance.javaClass.getDeclaredField("instanceManager").apply { isAccessible = true }.get(instance)

    instanceManager.javaClass.getDeclaredField("lookupNames")
        .apply { isAccessible = true }
        .get(instanceManager).let {
            @Suppress("UNCHECKED_CAST")
            val lookupNames = it as HashMap<String, org.bukkit.plugin.Plugin>
            val namesToRemove = ArrayList<String>()
            lookupNames.forEach { (key, value) ->
                if (Objects.equals(value, plugin)) {
                    namesToRemove.add(key)
                }
            }
            namesToRemove.forEach { key ->
                lookupNames.remove(key)
            }
        }
}