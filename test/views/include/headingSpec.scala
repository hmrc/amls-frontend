/*
 * Copyright 2021 HM Revenue & Customs
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
import utils.AmlsViewSpec
import views.html.include.heading

class headingSpec extends PlaySpec with AmlsViewSpec {

  trait Fixture {
  }

  "The Html output" must {
    "render the heading with title and section" in new Fixture {
      val result: String = heading("some title", "some section").toString
      val html: Document = Jsoup.parse(result)

      val h1: String = html.select("h1").text()
      val p: String = html.select("p").text()

      h1 mustBe "some title"
      p must include ("some section")
    }

    "render the heading with title only" in new Fixture {
      val result: String = heading("some title").toString
      val html: Document = Jsoup.parse(result)

      val h1: String = html.select("h1").text()
      val p: Elements = html.select("p" )

      h1 mustBe "some title"
      p.isEmpty mustBe true
    }
  }
}
