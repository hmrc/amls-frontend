/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessactivities.ExpectedAMLSTurnover
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import models.businessmatching.{AccountancyServices, BusinessActivities, BusinessMatching, EstateAgentBusinessService}
import play.api.i18n.Messages
import views.Fixture
import views.html.businessactivities.expected_amls_turnover


class expected_amls_turnoverSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val turnover = app.injector.instanceOf[expected_amls_turnover]
    implicit val requestWithToken = addTokenForView()
  }

  "expected_amls_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExpectedAMLSTurnover] = Form2(ExpectedAMLSTurnover.Fifth)

      def view = turnover(form2, true, None, None)

      doc.title must startWith(Messages("businessactivities.turnover.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct heading when one service is selected" in new ViewFixture {

      val form2: ValidForm[ExpectedAMLSTurnover] = Form2(ExpectedAMLSTurnover.Third)

      def view = turnover(form2, true, None, Some(List("accountancy service provider")))

      heading.html must be(Messages("businessactivities.turnover.heading", "accountancy service provider"))
      subHeading.html must include( Messages("summary.businessactivities"))

    }

    "have correct heading when multiple services are selected" in new ViewFixture {
      val businessMatching2 = BusinessMatching(activities = Some(BusinessActivities(Set(AccountancyServices, EstateAgentBusinessService))))

      val form2: ValidForm[ExpectedAMLSTurnover] = Form2(ExpectedAMLSTurnover.Third)

      def view = turnover(form2, true, None, Some(List("accountancy service provider", "estate agency business")))

      heading.html must be(Messages("businessactivities.turnover.heading.multiple"))
      subHeading.html must include( Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "expectedAMLSTurnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = turnover(form2, true, None, Some(List("some provider")))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("expectedAMLSTurnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {
      def view = turnover(EmptyForm, true, None, None)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
