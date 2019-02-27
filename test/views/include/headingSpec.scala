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

import forms.ValidField
import jto.validation.Path
import models.autocomplete.NameValuePair
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import utils.AmlsSpec
import views.html.include.forms2.country_autocomplete
import views.html.include.heading

class headingSpec extends PlaySpec with AmlsSpec {

  trait Fixture {
  }

  "The Html output" must {
    "render the heading with title and section" in new Fixture {
      val result = heading("some title", "some section").toString

      val html = Jsoup.parse(result)

      html.select("h1").text() mustBe "some title"
      html.select("p").text() contains "some section"
    }

    "render the heading with title only" in new Fixture {
      val result = heading("some title").toString

      val html = Jsoup.parse(result)

      html.select("h1").text() mustBe "some title"
      html.select("p" ).first() mustBe null
    }
  }
}
