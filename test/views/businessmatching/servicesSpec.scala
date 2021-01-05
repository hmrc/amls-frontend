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

package views.businessmatching

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessMatchingMsbServices, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.services


class servicesSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val services = app.injector.instanceOf[services]
    implicit val requestWithToken = addTokenForView()
  }

  "services view" must {
    "have correct title in when in presubmission mode " in new ViewFixture {

      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = services(form2, edit = true, isPreSubmission = true)

      doc.title must startWith(Messages("msb.services.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("msb.services.title"))
      subHeading.html must include(Messages("summary.businessmatching"))
    }

    "have correct title in when in non-presubmission mode " in new ViewFixture {

      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = services(form2, edit = true, isPreSubmission = false)

      doc.title must startWith(Messages("msb.services.title") + " - " + Messages("summary.updateinformation"))
      heading.html must be(Messages("msb.services.title"))
      subHeading.html must include(Messages("summary.updateinformation"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbServices") -> Seq(ValidationError("not a message Key"))
        ))

      def view = services(form2, edit = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("msbServices")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "hide the return to progress link" in new ViewFixture {
      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))

      def view = services(form2, edit = true, showReturnLink = false)
      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show the correct amount of checkboxes" in new ViewFixture {
      def view = services(EmptyForm, edit = false)
      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 4
    }

    "show the correct label for the checkboxes" in new ViewFixture {
      def view = services(EmptyForm, fxEnabledToggle = true, edit = false)
      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      val labels = Seq("03", "04", "02","05", "01")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).attr("value") mustEqual labels(i)
      }
    }

    "show selected checkboxes as checked" in new ViewFixture {
      val form2: ValidForm[BusinessMatchingMsbServices] = Form2(BusinessMatchingMsbServices(Set(TransmittingMoney)))
      def view = services(form2, edit = true)
      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).attr("checked") mustEqual (if (i == 3) "checked" else "")
      }
    }

    "have a back link in pre-submission mode" in new ViewFixture {
      def view = services(EmptyForm, edit = true, isPreSubmission = true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have a back link in non pre-submission mode" in new ViewFixture {
      def view = services(EmptyForm, edit = true, isPreSubmission = false)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}

class servicesFxEnabledSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "services fx-enabled view" must {
    lazy val services = app.injector.instanceOf[services]

    "show the correct amount of checkboxes" in new ViewFixture {
      def view = services(EmptyForm, edit = false, fxEnabledToggle = true)
      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 5
    }

    "show the correct label for the checkboxes" in new ViewFixture {
      def view = services(EmptyForm, edit = false, fxEnabledToggle = true)
      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      val labels = Seq("03", "04", "02","05", "01")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).attr("value") mustEqual labels(i)
      }
    }
  }
}