package com.example

import com.example.model.CheckStep
import com.example.model.HostCheckResult
import com.example.model.HostGroup
import com.example.model.HostStatus
import com.example.model.StepResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun verifyHostResultAttributes() {
    val result = HostCheckResult(
      host = "ya.ru",
      group = HostGroup.WHITELIST,
      status = HostStatus.AVAILABLE,
      totalLatencyMs = 120L
    )
    assertEquals("ya.ru", result.host)
    assertEquals(HostGroup.WHITELIST, result.group)
    assertEquals(HostStatus.AVAILABLE, result.status)
    assertEquals(120L, result.totalLatencyMs)
  }

  @Test
  fun verifyTimeoutAndNetworkErrorStatuses() {
    val timeoutResult = HostCheckResult(
      host = "example.com",
      group = HostGroup.CONTROL,
      status = HostStatus.TIMEOUT_ERROR,
      failedStep = CheckStep.TCP,
      errorMessage = "Connect timeout > 5000 ms"
    )
    assertEquals(HostStatus.TIMEOUT_ERROR, timeoutResult.status)
    assertEquals(CheckStep.TCP, timeoutResult.failedStep)

    val networkErrorResult = HostCheckResult(
      host = "invalid.domain.xyz",
      group = HostGroup.CUSTOM,
      status = HostStatus.NETWORK_ERROR,
      failedStep = CheckStep.DNS,
      errorMessage = "NXDOMAIN"
    )
    assertEquals(HostStatus.NETWORK_ERROR, networkErrorResult.status)
    assertEquals(CheckStep.DNS, networkErrorResult.failedStep)
  }
}
