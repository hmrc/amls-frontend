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

package views.businessmatching.updateservice.add

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberYes}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import views.html.businessmatching.updateservice.add.business_applied_for_psr_number


class business_applied_for_psr_numberSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    def view = business_applied_for_psr_number(EmptyForm, edit = false)
  }


  "The business_applied_for_psr_number view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

      override def view = views.html.businessmatching.business_applied_for_psr_number(form2, edit = false)

      doc.title must startWith(Messages("businessmatching.updateservice.psr.number.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("businessmatching.updateservice.psr.number.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "appliedFor") -> Seq(ValidationError("not a message Key")),
          (Path \ "regNumber-panel") -> Seq(ValidationError("second not a message Key"))
        ))

      override def view = business_applied_for_psr_number(form2, edit = false)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("appliedFor")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("regNumber-panel")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }

    "hide the return to progress link" in new ViewFixture {
      val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

      override def view = business_applied_for_psr_number(form2, edit = true, showReturnLink = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}
