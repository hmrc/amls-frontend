/*
 * Copyright 2018 HM Revenue & Customs
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
import utils.GenericTestHelper
import views.html.include.forms2.country_autocomplete

class country_autocompleteSpec extends PlaySpec with GenericTestHelper {

  trait Fixture {
    val listData = Seq(
      NameValuePair("Country 1", "country:1"),
      NameValuePair("Country 2", "country:2")
    )
  }

  "The Html output" must {
    "render a select box with the specified options" in new Fixture {
      val result = country_autocomplete(
        ValidField(Path \ "country", Seq("country:2")),
        data = listData
      ).toString

      val html = Jsoup.parse(result)

      listData foreach { item =>
        html.select(s"[value=${item.value}]").text() mustBe item.name
      }

      html.select(s"[selected]").attr("value") mustBe "country:2"
    }
  }

}
