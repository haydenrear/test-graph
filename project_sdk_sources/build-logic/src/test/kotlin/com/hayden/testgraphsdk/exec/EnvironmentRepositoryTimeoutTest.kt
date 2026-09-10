package com.hayden.testgraphsdk.exec

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The environment-repository command budget was one hard-coded 5 minutes for
 * every command, shorter than the cluster-creation budget consuming repositories
 * declare for the same `tofu apply`. These pin the resolution order.
 */
class EnvironmentRepositoryTimeoutTest {

    @Test
    fun `falls back to the default when nothing is set`() {
        assertEquals(5 * 60 * 1000L, EnvironmentRepositoryRuntime.resolveTimeoutMillis(emptyMap()))
    }

    @Test
    fun `global override is read in seconds`() {
        val env = mapOf(EnvironmentRepositoryRuntime.TIMEOUT_ENV to "720")
        assertEquals(720_000L, EnvironmentRepositoryRuntime.resolveTimeoutMillis(env))
    }

    @Test
    fun `a per-command override beats the global one`() {
        val env = mapOf(
            EnvironmentRepositoryRuntime.TIMEOUT_ENV to "600",
            "TEST_GRAPH_ENVIRONMENT_TIMEOUT_SECONDS_TOFU_APPLY" to "1800",
        )
        assertEquals(
            1_800_000L,
            EnvironmentRepositoryRuntime.resolveCommandTimeoutMillis(env, "tofu-apply", 600_000L),
        )
    }

    @Test
    fun `a command with no override keeps the surrounding budget`() {
        val env = mapOf("TEST_GRAPH_ENVIRONMENT_TIMEOUT_SECONDS_TOFU_APPLY" to "1800")
        assertEquals(
            600_000L,
            EnvironmentRepositoryRuntime.resolveCommandTimeoutMillis(env, "git-clone", 600_000L),
        )
    }

    @Test
    fun `command labels become env names`() {
        assertEquals(
            "TEST_GRAPH_ENVIRONMENT_TIMEOUT_SECONDS_TOFU_APPLY",
            EnvironmentRepositoryRuntime.commandTimeoutEnvName("tofu-apply"),
        )
    }

    @Test
    fun `nonsense and non-positive values fall back rather than disabling the timeout`() {
        for (bad in listOf("", "   ", "abc", "0", "-1")) {
            val env = mapOf(EnvironmentRepositoryRuntime.TIMEOUT_ENV to bad)
            assertEquals(
                5 * 60 * 1000L,
                EnvironmentRepositoryRuntime.resolveTimeoutMillis(env),
                "value $bad should fall back",
            )
        }
    }
}
