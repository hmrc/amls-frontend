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
import models.asp.{OtherBusinessTaxMattersNo, OtherBusinessTaxMattersYes, OtherBusinessTaxMatters}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class other_business_tax_mattersSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "other_business_tax_matters view" must {


    "have correct title" in new ViewFixture {

      val form2: ValidForm[OtherBusinessTaxMatters] = Form2(OtherBusinessTaxMattersYes)

      def view = views.html.asp.other_business_tax_matters(form2, true)

      doc.title must startWith(Messages("asp.other.business.tax.matters.title") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[OtherBusinessTaxMatters] = Form2(OtherBusinessTaxMattersNo)

      def view = views.html.asp.other_business_tax_matters(form2, true)

      heading.html must be(Messages("asp.other.business.tax.matters.title"))
      subHeading.html must include(Messages("summary.asp"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "otherBusinessTaxMatters") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.asp.other_business_tax_matters(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("otherBusinessTaxMatters")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
