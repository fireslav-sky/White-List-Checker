package com.fireslav.whitelistchecker

import com.fireslav.whitelistchecker.model.HostCheckResult
import com.fireslav.whitelistchecker.model.HostGroup
import com.fireslav.whitelistchecker.model.HostStatus
import com.fireslav.whitelistchecker.model.VerdictEvaluator
import com.fireslav.whitelistchecker.model.VerdictType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VerdictEvaluatorTest {

  private fun hosts(group: HostGroup, up: Int, down: Int, downStatus: HostStatus = HostStatus.UNAVAILABLE) =
    List(up) { HostCheckResult("${group.name.lowercase()}-up-$it", group, HostStatus.AVAILABLE) } +
      List(down) { HostCheckResult("${group.name.lowercase()}-down-$it", group, downStatus) }

  private fun verdictOf(vararg groups: List<HostCheckResult>) = VerdictEvaluator.evaluate(groups.toList().flatten()).type

  @Test
  fun `whitelist up and control down means whitelist mode`() {
    assertEquals(VerdictType.WHITELIST_ACTIVE, verdictOf(hosts(HostGroup.WHITELIST, 5, 0), hosts(HostGroup.CONTROL, 0, 5)))
  }

  @Test
  fun `everything up means no restrictions`() {
    assertEquals(VerdictType.NO_RESTRICTIONS, verdictOf(hosts(HostGroup.WHITELIST, 5, 0), hosts(HostGroup.CONTROL, 5, 0)))
  }

  @Test
  fun `everything down means no connection`() {
    assertEquals(
      VerdictType.NO_CONNECTION,
      verdictOf(hosts(HostGroup.WHITELIST, 0, 5, HostStatus.TIMEOUT_ERROR), hosts(HostGroup.CONTROL, 0, 5, HostStatus.NETWORK_ERROR))
    )
  }

  @Test
  fun `half available is mixed`() {
    assertEquals(VerdictType.MIXED, verdictOf(hosts(HostGroup.WHITELIST, 5, 5), hosts(HostGroup.CONTROL, 5, 5)))
  }

  @Test
  fun `threshold boundaries are inclusive`() {
    // 7/10 = 0.70 белых и 3/10 = 0.30 контрольных — ещё режим белых списков.
    assertEquals(VerdictType.WHITELIST_ACTIVE, verdictOf(hosts(HostGroup.WHITELIST, 7, 3), hosts(HostGroup.CONTROL, 3, 7)))
  }

  @Test
  fun `no control hosts does not report whitelist mode`() {
    assertEquals(VerdictType.PENDING, verdictOf(hosts(HostGroup.WHITELIST, 5, 0)))
  }

  @Test
  fun `no whitelist hosts gives no verdict`() {
    assertEquals(VerdictType.PENDING, verdictOf(hosts(HostGroup.CONTROL, 0, 5)))
  }

  @Test
  fun `empty results give no verdict`() {
    assertEquals(VerdictType.PENDING, VerdictEvaluator.evaluate(emptyList()).type)
  }

  @Test
  fun `custom hosts do not affect verdict`() {
    assertEquals(
      VerdictType.NO_RESTRICTIONS,
      verdictOf(hosts(HostGroup.WHITELIST, 5, 0), hosts(HostGroup.CONTROL, 5, 0), hosts(HostGroup.CUSTOM, 0, 10))
    )
    assertEquals(VerdictType.PENDING, verdictOf(hosts(HostGroup.WHITELIST, 5, 0), hosts(HostGroup.CUSTOM, 0, 10)))
  }

  @Test
  fun `slow hosts count as available`() {
    assertTrue(VerdictEvaluator.isAvailable(HostStatus.SLOW))
    assertTrue(VerdictEvaluator.isAvailable(HostStatus.AVAILABLE))
    assertFalse(VerdictEvaluator.isAvailable(HostStatus.CHECKING))
    assertFalse(VerdictEvaluator.isAvailable(HostStatus.TIMEOUT_ERROR))
  }
}
