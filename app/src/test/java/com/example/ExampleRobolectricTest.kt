package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.command.CommandParser
import com.example.command.LocalCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("EZE", appName)
  }

  @Test
  fun `test local calculator evaluation`() {
    val result = LocalCalculator.evaluate("25 times 8")
    assertNotNull(result)
    assertEquals("25 × 8 = 200", result)

    val convResult = LocalCalculator.evaluate("convert 5 km to miles")
    assertNotNull(convResult)
  }

  @Test
  fun `test command parser alarm`() {
    val cmd = CommandParser.parse("Set an alarm for 6:30 AM")
    assertEquals(com.example.command.ActionType.SET_ALARM, cmd.actionType)
    assertEquals("6", cmd.parameters["hour"])
    assertEquals("30", cmd.parameters["minutes"])
  }
}
