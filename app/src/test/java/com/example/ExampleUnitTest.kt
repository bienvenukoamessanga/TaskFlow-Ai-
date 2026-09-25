package com.example

import com.example.model.SubTask
import com.example.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun task_fieldsExistAndAccessible() {
    val task = Task(
      id = 1L,
      title = "Acheter des fournitures",
      description = "Papier, stylos et classeurs",
      dueDate = 1758765432000L,
      isCompleted = false
    )
    assertEquals(1L, task.id)
    assertEquals("Acheter des fournitures", task.title)
    assertEquals("Papier, stylos et classeurs", task.description)
    assertEquals(1758765432000L, task.dueDate)
    assertFalse(task.isCompleted)
    assertFalse(task.completionStatus)
  }

  @Test
  fun task_completionStatusReflectsCompletedState() {
    val task = Task(
      id = 2L,
      title = "Rapport final",
      description = "Envoyé par email",
      dueDate = null,
      isCompleted = true
    )
    assertTrue(task.isCompleted)
    assertTrue(task.completionStatus)
  }

  @Test
  fun taskItem_progressCalculatesCorrectly() {
    val subtasks = listOf(
      SubTask(title = "Sub 1", isDone = true),
      SubTask(title = "Sub 2", isDone = false)
    )
    val task = Task(title = "Test Task", subtasks = subtasks)
    assertEquals(0.5f, task.progressPercent, 0.001f)
    assertEquals(1, task.completedSubtasksCount)
    assertEquals(2, task.totalSubtasksCount)
  }
}
