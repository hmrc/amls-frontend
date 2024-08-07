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

package models.registrationprogress

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag

case class TaskList(rows: Seq[TaskRow])
case class TaskRow(msgKey: String, href: String, hasChanged: Boolean = false, status: Status, tag: Tag)

object TaskRow {

  def completedTag(implicit messages: Messages): Tag = Tag(
    Text(messages("status.complete")),
    "govuk-tag registration-status-tag"
  )

  def updatedTag(implicit messages: Messages): Tag = Tag(
    Text(messages("status.updated")),
    "govuk-tag govuk-tag--blue registration-status-tag"
  )

  def incompleteTag(implicit messages: Messages): Tag = Tag(
    Text(messages("status.incomplete")),
    "govuk-tag govuk-tag--blue registration-status-tag"
  )

  def notStartedTag(implicit messages: Messages): Tag = Tag(
    Text(messages("status.notstarted")),
    "govuk-tag govuk-tag--light-blue registration-status-tag"
  )
}
