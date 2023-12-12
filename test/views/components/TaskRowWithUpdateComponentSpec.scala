/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.components

import models.registrationprogress.TaskRow.{completedTag, incompleteTag, notStartedTag, updatedTag}
import models.registrationprogress.{Completed, NotStarted, Started, TaskRow, Updated}
import org.jsoup.Jsoup
import utils.AmlsViewSpec
import views.html.components.TaskRowWithUpdateComponent

class TaskRowWithUpdateComponentSpec extends AmlsViewSpec {

  lazy val injectedView: TaskRowWithUpdateComponent = inject[TaskRowWithUpdateComponent]

  "The task row status tag text" should {

    "be 'Updated'" when {

      "the status is Updated and existing data has changed" in {
        val taskRow = TaskRow("", "", hasChanged = true, Updated, updatedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Updated"
      }

      "the status is Completed and existing data has changed" in {
        val taskRow = TaskRow("", "", hasChanged = true, Completed, completedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Updated"
      }
    }

    "be 'Completed'" when {

      "the status is Updated and there was no existing data" in {
        val taskRow = TaskRow("", "", hasChanged = false, Updated, updatedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Completed"
      }

      "the status is Completed and there was no existing data" in {
        val taskRow = TaskRow("", "", hasChanged = false, Completed, completedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Completed"
      }
    }

    "be 'Incomplete'" when {

      "the status is Started and existing data has changed" in {
        val taskRow = TaskRow("", "", hasChanged = true, Started, incompleteTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Incomplete"
      }

      "the status is Not Started and existing data has changed" in {
        val taskRow = TaskRow("", "", hasChanged = true, NotStarted, notStartedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Incomplete"
      }
    }

    "be 'Not started'" when {

      "the status is Not Started and there was no existing data" in {
        val taskRow = TaskRow("", "", hasChanged = false, NotStarted, notStartedTag)
        val document = Jsoup.parse(injectedView(taskRow).body)
        document.select("li > strong").text() mustBe "Not started"
      }
    }
  }
}
