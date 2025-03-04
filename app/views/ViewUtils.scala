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

import models.registrationprogress.{Completed, NotStarted, Started, TaskRow, Updated}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{TaskListItem, TaskListItemStatus, TaskListItemTitle}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

object ViewUtils {

  def errorTitlePrefix(content: Html)(implicit messages: Messages): String =
    if (content.toString().contains("govuk-error-summary")) messages("error.browser.title.prefix").concat(" ") else ""

  def asTaskListItem(taskRow: TaskRow)(implicit messages: Messages): TaskListItem = {

    val (title, status) = taskRow.status match {
      case Completed  =>
        (
          messages("status.progress.edit", sectionName(taskRow.msgKey)),
          TaskListItemStatus(None, Text(messages("status.complete")))
        )
      case NotStarted =>
        (
          messages("status.progress.add", sectionName(taskRow.msgKey)),
          TaskListItemStatus(Some(TaskRow.notStartedTag))
        )
      case Started    =>
        (
          messages("status.progress.add", sectionName(taskRow.msgKey)),
          TaskListItemStatus(Some(TaskRow.incompleteTag))
        )
      case _          =>
        (
          messages("status.progress.manage", sectionName(taskRow.msgKey)),
          TaskListItemStatus(None, Text(messages("status.complete")))
        )
    }

    TaskListItem(TaskListItemTitle(Text(title)), None, status, Some(taskRow.href), s"${taskRow.msgKey}-task-list-item")
  }

  def asTaskListItemUpdate(taskRow: TaskRow)(implicit messages: Messages): TaskListItem = {

    val isCompletedOrUpdated = taskRow.status == Updated || taskRow.status == Completed

    val title = if (isCompletedOrUpdated) {
      messages("status.progress.manage", sectionName(taskRow.msgKey))
    } else {
      messages("status.progress.add", sectionName(taskRow.msgKey))
    }

    val status = (isCompletedOrUpdated, taskRow.hasChanged) match {
      case (true, true)   => TaskListItemStatus(Some(TaskRow.updatedTag))
      case (true, false)  => TaskListItemStatus(None, Text(messages("status.complete")))
      case (false, true)  => TaskListItemStatus(Some(TaskRow.incompleteTag))
      case (false, false) => TaskListItemStatus(Some(taskRow.tag))
    }

    TaskListItem(TaskListItemTitle(Text(title)), None, status, Some(taskRow.href), s"${taskRow.msgKey}-task-list-item")
  }

  private def sectionName(str: String)(implicit messages: Messages) = messages(s"progress.$str.name")
}
