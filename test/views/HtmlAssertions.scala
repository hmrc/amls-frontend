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

import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages

import scala.jdk.CollectionConverters._

trait HtmlAssertions extends MockitoSugar {
  self: Matchers =>

  def checkListContainsItems(parent: Element, keysToFind: Set[String])(implicit messages: Messages) = {
    val texts = parent.select("li").asScala.map((el: Element) => el.text())
    texts must be(keysToFind.map(k => Messages(k)))
    true
  }

  def checkElementTextIncludes(el: Element, keys: String*)(implicit messages: Messages) = {
    val t = el.text()
    val l = el.getElementsByTag("a").attr("href")
    val p = l.substring(l.indexOf("?"))
    keys.foreach { k =>
      t must include(Messages(k))
      p must include("edit=true")
    }
    true
  }

  def checkElementTextOnlyIncludes(el: Element, keys: String*)(implicit messages: Messages) = {
    val t = el.text()
    keys.foreach { k =>
      t must include(Messages(k))
    }
    true
  }
}
