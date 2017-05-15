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

package views.asp

import forms.{InvalidForm, ValidForm, Form2}
import models.asp._
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class services_of_businessSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "services_of_business view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ServicesOfBusiness] = Form2(ServicesOfBusiness(
                                                    Set(Accountancy,
                                                      PayrollServices,
                                                      BookKeeping,
                                                      Auditing,
                                                      FinancialOrTaxAdvice)))

      def view = views.html.asp.services_of_business(form2, true)

      doc.title must startWith(Messages("asp.services.title") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ServicesOfBusiness] = Form2(ServicesOfBusiness(
                                                    Set(Accountancy,
                                                      PayrollServices,
                                                      BookKeeping,
                                                      Auditing,
                                                      FinancialOrTaxAdvice)))

      def view = views.html.asp.services_of_business(form2, true)

      heading.html must be(Messages("asp.services.title"))
      subHeading.html must include(Messages("summary.asp"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "services") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.asp.services_of_business(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("services")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
