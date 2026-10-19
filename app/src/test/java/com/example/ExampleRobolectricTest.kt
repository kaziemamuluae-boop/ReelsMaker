package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AspectRatioPreset
import com.example.model.ProjectEditState
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
    assertEquals("Reels Maker", appName)
  }

  @Test
  fun `verify default project state initialization`() {
    val state = ProjectEditState(
      projectId = "test-123",
      projectName = "Sample Reel",
      aspectRatio = AspectRatioPreset.RATIO_9_16
    )
    assertEquals("test-123", state.projectId)
    assertEquals("Sample Reel", state.projectName)
    assertEquals(AspectRatioPreset.RATIO_9_16, state.aspectRatio)
    assertEquals(9f / 16f, state.aspectRatio.ratio, 0.001f)
  }
}

