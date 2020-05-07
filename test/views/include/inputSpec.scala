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

import forms.{EmptyForm, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import org.jsoup.Jsoup
import org.scalatestplus.play.PlaySpec
import utils.AmlsViewSpec
import play.api.test.Helpers._

class inputSpec extends PlaySpec with AmlsViewSpec {

  "The Html output" must {
    "render the ariaDescribedBy with error and hint" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "foo", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("foo" -> Seq("bar")), errors)
      val field = invalidForm("foo")
      val input = views.html.include.forms2.input(field,  hintText = "select all")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-hint foo-error-notification")
    }

    "render the ariaDescribedBy with error and hint and attrDescribedBy" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "foo", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("foo" -> Seq("bar")), errors)
      val field = invalidForm("foo")
      val input = views.html.include.forms2.input(field,  hintText = "select all", attrDescribedBy = "describe")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-hint foo-error-notification describe-hint")
    }

    "render the ariaDescribedBy with error" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "foo", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("foo" -> Seq("bar")), errors)
      val field = invalidForm("foo")
      val input = views.html.include.forms2.input(field)
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-error-notification")
    }

    "render the ariaDescribedBy with error and attrDescribedBy" in {
      val errors: Seq[(Path, Seq[ValidationError])] = Seq((Path \ "foo", Seq(ValidationError("some.error", ""))))
      val invalidForm = InvalidForm(Map("foo" -> Seq("bar")), errors)
      val field = invalidForm("foo")
      val input = views.html.include.forms2.input(field, attrDescribedBy = "describe")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-error-notification describe-hint")
    }

    "render the ariaDescribedBy with hint" in {
      val validForm = ValidForm(Map("foo" -> Seq("bar")), EmptyForm)
      val field = validForm("foo")
      val input = views.html.include.forms2.input(field, hintText = "select all")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-hint")
    }

    "render the ariaDescribedBy with hint and attrDescribedBy" in {
      val validForm = ValidForm(Map("foo" -> Seq("bar")), EmptyForm)
      val field = validForm("foo")
      val input = views.html.include.forms2.input(field, hintText = "select all", attrDescribedBy = "describe")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("foo-hint describe-hint")
    }

    "render the ariaDescribedBy with attrDescribedBy" in {
      val validForm = ValidForm(Map("foo" -> Seq("bar")), EmptyForm)
      val field = validForm("foo")
      val input = views.html.include.forms2.input(field, attrDescribedBy = "describe")
      val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
      aria must be("describe-hint")
    }

    "where no ariaDescribedBy params" must {
      "not render the ariaDescribedBy" in {
        val validForm = ValidForm(Map("foo" -> Seq("bar")), EmptyForm)
        val field = validForm("foo")
        val input = views.html.include.forms2.input(field)
        val aria = Jsoup.parse(contentAsString(input)).getElementsByTag("input").attr("aria-describedby")
        aria must be("")
      }
    }
  }
}
