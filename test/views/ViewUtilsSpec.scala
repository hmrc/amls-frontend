/*
 * Copyright 2024 HM Revenue & Customs
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

package views

import models.registrationprogress._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.TaskListItemStatus
import utils.AmlsSpec

class ViewUtilsSpec extends AmlsSpec {

  val url = "/foo"

  def taskRow(status: Status, tag: Tag = TaskRow.notStartedTag, hasChanged: Boolean = false): TaskRow =
    TaskRow("amp", url, hasChanged, status, tag)

  "ViewUtils" when {

    ".errorTitlePrefix is called" must {

      "add an error prefix if HTML body contains an error" in {

        val html = Html(
          """
            |<div class="govuk-error-summary">
            |   <p>There was a problem</p>
            |</div>
            |""".stripMargin
        )

        s"${ViewUtils.errorTitlePrefix(html)}Title" mustBe "Error: Title"
      }

      "not change markup when no error summary is present" in {

        val html = Html(
          """
            |<div class="govuk-body">
            |   <p>There was no problem</p>
            |</div>
            |""".stripMargin
        )

        s"${ViewUtils.errorTitlePrefix(html)} Title" must not include "Error:"
      }
    }

    ".asTaskListItem is called" must {

      "return the correct TaskListItem" when {

        Seq(
          (taskRow(NotStarted), "Add", TaskListItemStatus(Some(TaskRow.notStartedTag))),
          (taskRow(Started), "Add", TaskListItemStatus(Some(TaskRow.incompleteTag))),
          (taskRow(Completed), "Edit", TaskListItemStatus(None, Text("Completed"))),
          (taskRow(Updated), "Manage", TaskListItemStatus(None, Text("Completed")))
        ) foreach { case (row, prefix, status) =>
          s"status is ${row.status}" in {

            val result = ViewUtils.asTaskListItem(row)

            result.title.content.asHtml.toString() must include(s"$prefix art market participant")
            result.status mustBe status
            result.href mustBe Some(url)
          }
        }
      }
    }

    ".asTaskListItemUpdate is called" must {

      "return the correct TaskListItem" when {

        Seq(
          (taskRow(NotStarted), "Add", TaskListItemStatus(Some(TaskRow.notStartedTag))),
          (taskRow(NotStarted, hasChanged = true), "Add", TaskListItemStatus(Some(TaskRow.incompleteTag))),
          (taskRow(Started, TaskRow.incompleteTag), "Add", TaskListItemStatus(Some(TaskRow.incompleteTag))),
          (taskRow(Started, hasChanged = true), "Add", TaskListItemStatus(Some(TaskRow.incompleteTag))),
          (taskRow(Completed), "Manage", TaskListItemStatus(None, Text("Completed"))),
          (taskRow(Completed, hasChanged = true), "Manage", TaskListItemStatus(Some(TaskRow.updatedTag))),
          (taskRow(Updated), "Manage", TaskListItemStatus(None, Text("Completed"))),
          (taskRow(Updated, hasChanged = true), "Manage", TaskListItemStatus(Some(TaskRow.updatedTag)))
        ) foreach { case (row, expectedPrefix, expectedStatus) =>
          s"status is ${row.status} and hasChanged is ${row.hasChanged}" in {

            val result = ViewUtils.asTaskListItemUpdate(row)

            result.title.content.asHtml.toString() must include(s"$expectedPrefix art market participant")
            result.status mustBe expectedStatus
            result.href mustBe Some(url)
          }
        }
      }
    }
  }
}
