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

package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{CorporationTaxRegistered, CorporationTaxRegisteredYes}
import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class corporation_tax_registeredSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "corporation_tax_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorporationTaxRegistered] = Form2(CorporationTaxRegisteredYes("1234567890"))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.registeredforcorporationtax.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorporationTaxRegistered] = Form2(CorporationTaxRegisteredYes("1234567890"))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      heading.html must be(Messages("aboutthebusiness.registeredforcorporationtax.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registeredForCorporationTax") -> Seq(ValidationError("not a message Key")),
          (Path \ "corporationTaxReference-panel") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("registeredForCorporationTax")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("corporationTaxReference-panel")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
