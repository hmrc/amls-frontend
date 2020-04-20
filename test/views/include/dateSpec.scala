/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatestplus.play.PlaySpec
import utils.AmlsViewSpec
import views.html.include.heading
import play.api.test.Helpers._

class dateSpec extends PlaySpec with AmlsViewSpec {

  trait Fixture {
  }

  "The Html output" must {
    "render the ariaDescribedBy with error and hint" in new Fixture {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "date", Seq(ValidationError("some.error", ""))))
      val formx = InvalidForm(Map("" -> Seq("")), errors)
      val date = views.html.include.forms2.date(formx, p="date", hintText = "jhgjhgjgkjkjk")

      val aria = Jsoup.parse(contentAsString(date)).getElementsByTag("fieldset").attr("aria-describedby")
      aria must not be "date-hintdate-error-notification"

    }
  }
}
