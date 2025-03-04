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

package utils

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, HtmlContent, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key

trait CheckYourAnswersHelperFunctions {

  def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
    messages("lbl.yes")
  } else {
    messages("lbl.no")
  }

  def toBulletList[A](coll: Seq[A]): Value = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list govuk-list--bullet\">" +
          coll.map { x =>
            s"<li>$x</li>"
          }.mkString +
          "</ul>"
      )
    )
  )

  def addressToLines(addressLines: Seq[String]): Value = Value(
    HtmlContent(
      Html(
        "<ul class=\"govuk-list\">" +
          addressLines.map { line =>
            s"""<li>$line<li>"""
          }.mkString
          + "</ul>"
      )
    )
  )

  def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )

  def editAction(route: String, hiddenText: String, id: String)(implicit messages: Messages): Option[Actions] =
    Some(
      Actions(
        items = Seq(
          ActionItem(
            route,
            Text(messages("button.edit")),
            visuallyHiddenText = Some(messages(hiddenText)),
            attributes = Map("id" -> id)
          )
        )
      )
    )
}
