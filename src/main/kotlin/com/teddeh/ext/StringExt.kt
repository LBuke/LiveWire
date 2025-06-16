package com.teddeh.ext

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

fun String.comp() : Component = MiniMessage.miniMessage().deserialize(this)