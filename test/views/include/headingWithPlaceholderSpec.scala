/*
 * Copyright 2019 HM Revenue & Customs
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

package views.include

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import utils.AmlsSpec
import views.html.include.headingWithPlaceholder

class headingWithPlaceholderSpec extends PlaySpec with AmlsSpec {

  "The Html output" must {
    "render the heading with title and placeholder provided" in {
      val result: String = headingWithPlaceholder(("responsiblepeople.remove.named.responsible.person", "rpName")).toString
      val html: Document = Jsoup.parse(result)

      val header: String = html.select( "h1").text()
      val section: Elements = html.select("p")

      header must include ("rpName")
      section.isEmpty mustBe true
    }

    "render the section" in {
      val result: String = headingWithPlaceholder(("dontCare", "dontCare"), "mySection").toString
      val html: Document = Jsoup.parse(result)

      val section: String = html.select( "p").text()

      section must include ("mySection")
    }
  }
}
