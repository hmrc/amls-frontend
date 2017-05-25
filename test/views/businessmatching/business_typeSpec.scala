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
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.LimitedCompany
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class business_typeSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "business_type view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessType] = Form2(LimitedCompany)

      def view = views.html.businessmatching.business_type(form2)

      doc.title must startWith(Messages("businessmatching.businessType.title") + " - " + Messages("summary.businessmatching"))
      heading.html must be(Messages("businessmatching.businessType.title"))
      subHeading.html must include(Messages("summary.businessmatching"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "businessType") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.business_type(form2)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("businessType")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}