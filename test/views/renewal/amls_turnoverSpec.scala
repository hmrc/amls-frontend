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

package views.renewal

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{AccountancyServices, BusinessActivities}
import models.renewal.AMLSTurnover
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class amls_turnoverSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "amls_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Fifth)

      def view = views.html.renewal.amls_turnover(form2, true, None)

      doc.title must startWith(Messages("renewal.turnover.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Third)

      def view = views.html.renewal.amls_turnover(form2, true, None)

      heading.html must be(Messages("renewal.turnover.title"))
      subHeading.html must include( Messages("summary.renewal"))

    }

    "correctly list business activities" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Fifth)

      def view = views.html.renewal.amls_turnover(form2, true, Some(BusinessActivities(Set(AccountancyServices))))

      html must include("Accountancy services")
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "turnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.amls_turnover(form2, true, None)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("turnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
