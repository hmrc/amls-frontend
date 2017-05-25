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

package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.{BusinessAppliedForPSRNumberYes, BusinessAppliedForPSRNumber}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class business_applied_for_psr_numberSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }


  "business_applied_for_psr_number view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

      def view = views.html.businessmatching.business_applied_for_psr_number(form2, true)

      doc.title must startWith(Messages("businessmatching.psr.number.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("businessmatching.psr.number.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "appliedFor") -> Seq(ValidationError("not a message Key")),
          (Path \ "regNumber-panel") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessmatching.business_applied_for_psr_number(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("appliedFor")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("regNumber-panel")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
