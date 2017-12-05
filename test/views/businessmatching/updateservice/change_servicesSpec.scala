/*
 * Copyright 2017 HM Revenue & Customs
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

package views.businessmatching.updateservice

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.LimitedCompany
import models.businessmatching.updateservice.{ChangeServices, ChangeServicesAdd}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class change_servicesSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "change_services view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.businessmatching.updateservice.change_services(EmptyForm,Set.empty[String])

      doc.title must startWith(Messages("changeservices.title") + " - " + Messages("summary.updateinformation"))
      heading.html must be(Messages("changeservices.title"))
      subHeading.html must include(Messages("summary.updateinformation"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "changeServices") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.updateservice.change_services(form2, Set.empty[String])

      errorSummary.html() must include("not a message Key")

      doc.getElementById("changeServices")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "not show the return link when specified" in new ViewFixture {
      def view = views.html.businessmatching.updateservice.change_services(EmptyForm,Set.empty[String])

      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}