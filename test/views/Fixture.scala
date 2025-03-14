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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.Strings.TextHelpers

import scala.jdk.CollectionConverters._

trait ParagraphHelpers extends Matchers {
  def validateParagraphizedContent(messageKey: String)(implicit messages: Messages, doc: Document): Unit =
    for (p <- Jsoup.parse(messages(messageKey).paragraphize).getElementsByTag("p").asScala)
      doc.body().toString must include(p.text())
}

trait Fixture extends Matchers with ParagraphHelpers with TitleValidator {

  def view: HtmlFormat.Appendable
  lazy val html                   = view.body
  implicit lazy val doc: Document = Jsoup.parse(html)
  lazy val form                   = doc.getElementsByTag("form").first()
  lazy val heading                = doc.getElementsByTag("h1").first()
  lazy val caption                = doc.getElementById("stage-caption")
  lazy val subHeading             = doc.getElementsByClass("heading-secondary").first()
  lazy val errorSummary           = doc.getElementsByClass("amls-error-summary").first()
}
