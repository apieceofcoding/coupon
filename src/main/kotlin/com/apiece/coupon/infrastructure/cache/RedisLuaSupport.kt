package com.apiece.coupon.infrastructure.cache

import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript

fun listLuaScript(path: String): RedisScript<List<*>> =
    RedisScript.of(ClassPathResource(path), List::class.java)

fun longLuaScript(path: String): RedisScript<Long> =
    RedisScript.of(ClassPathResource(path), Long::class.java)

@Suppress("UNCHECKED_CAST")
fun StringRedisTemplate.runForStrings(
    script: RedisScript<List<*>>,
    keys: List<String>,
    vararg args: Any,
): List<String> = execute(script, keys, *args.map { it.toString() }.toTypedArray()) as List<String>

fun StringRedisTemplate.runForLong(
    script: RedisScript<Long>,
    keys: List<String>,
    vararg args: Any,
): Long = execute(script, keys, *args.map { it.toString() }.toTypedArray())
    ?: error("Lua 결과 null")
