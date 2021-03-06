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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import utils.AmlsViewSpec
import views.html.include.heading
import play.api.test.Helpers._

class dateSpec extends PlaySpec with AmlsViewSpec {

  "The Html output" must {
    "render the ariaDescribedBy with error and hint" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "date", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("" -> Seq("")), errors)
      val date = views.html.include.forms2.date(invalidForm, p = "date", hintText = "select all")
      val aria = Jsoup.parse(contentAsString(date)).getElementsByTag("fieldset").attr("aria-describedby")
      aria must be("date-hint date-error-notification")
    }
    "render the ariaDescribedBy with error" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "date", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("" -> Seq("")), errors)
      val date = views.html.include.forms2.date(invalidForm, p = "date")
      val aria = Jsoup.parse(contentAsString(date)).getElementsByTag("fieldset").attr("aria-describedby")
      aria must be("date-error-notification")
    }
    "render the ariaDescribedBy with hint" in {
      val validForm = ValidForm(Map("" -> Seq("")), EmptyForm)
      val date = views.html.include.forms2.date(validForm, p = "date", hintText = "select all")
      val aria = Jsoup.parse(contentAsString(date)).getElementsByTag("fieldset").attr("aria-describedby")
      aria must be("date-hint")
    }
  }
}
