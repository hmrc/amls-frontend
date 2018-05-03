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

package views.businessactivities

import forms.{InvalidForm, EmptyForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessMatching
import org.scalatest.{MustMatchers}
import utils.AmlsSpec
import play.api.i18n.Messages
import views.Fixture


class involved_in_other_nameSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  val bmModel = BusinessMatching()

  "involved_in_other_name view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      doc.title must be(Messages("businessactivities.involved.other.title") + " - " +
        Messages("summary.businessactivities") +
      " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      heading.html must be(Messages("businessactivities.involved.other.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      doc.getElementsByAttributeValue("name", "involvedInOther") must not be empty
      doc.getElementsByAttributeValue("name", "details") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "involvedInOther") -> Seq(ValidationError("not a message Key")),
          (Path \ "details") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

    }
  }
}
