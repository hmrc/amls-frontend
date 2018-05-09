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

package views.estateagentbusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.estateagentbusiness._
import org.scalatest.{MustMatchers}
import  utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class business_servicessSpec extends AmlsSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "business_servicess view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Services] = Form2(Services(Set()))

      def view = views.html.estateagentbusiness.business_servicess(form2, edit = true)

      doc.title must startWith(Messages("estateagentbusiness.services.title") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[Services] = Form2(Services(Set()))

      def view = views.html.estateagentbusiness.business_servicess(form2, edit = true)

      heading.html must be(Messages("estateagentbusiness.services.title"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "services") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.estateagentbusiness.business_servicess(form2, edit = true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("services")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}