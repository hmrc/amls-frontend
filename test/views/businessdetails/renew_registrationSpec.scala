/*
 * Copyright 2023 HM Revenue & Customs
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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.declaration.{RenewRegistration, RenewRegistrationYes}
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import views.Fixture
import utils.{AmlsViewSpec, DateHelper}
import views.html.declaration.renew_registration


class renew_registrationSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val renew_registration = app.injector.instanceOf[renew_registration]
    implicit val requestWithToken = addTokenForView()
  }

  "renew_registration view" must {
    "have correct title" in new ViewFixture {
      val form2: ValidForm[RenewRegistration] = Form2(RenewRegistrationYes)

      def view = renew_registration(form2,
        Some(LocalDate.parse("2019-3-20")))

      doc.title must startWith(Messages("declaration.renew.registration.title") + " - " + Messages("summary.declaration"))
    }

    "have correct headings" in new ViewFixture {
      val form2: ValidForm[RenewRegistration] = Form2(RenewRegistrationYes)

      def view = renew_registration(
        form2, Some(LocalDate.parse("2019-3-20"))
      )

      heading.html must be(Messages("declaration.renew.registration.title"))
      subHeading.html must include(Messages("summary.declaration"))
    }

    "have correct sections" in new ViewFixture {
      val form2: ValidForm[RenewRegistration] = Form2(RenewRegistrationYes)

      def view = renew_registration(
        form2, Some(LocalDate.parse("2019-3-20"))
      )

      doc.html must include(Messages("declaration.renew.registration.section1"))
      doc.html must include(Messages("declaration.renew.registration.section2",
        DateHelper.formatDate(LocalDate.parse("2019-3-20"))))
      doc.html must include(Messages("declaration.renew.now"))
      doc.html must include(Messages("declaration.continue.update"))
    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "renewRegistration") -> Seq(ValidationError("not a message Key"))
        ))

      def view = renew_registration(
        form2, Some(LocalDate.parse("2019-3-20"))
      )

      errorSummary.html() must include("not a message Key")

      doc.getElementById("renewRegistration")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = renew_registration(
        form2, Some(LocalDate.parse("2019-3-20"))
      )

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
