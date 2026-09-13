package io.github.tobsef.svgokt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform