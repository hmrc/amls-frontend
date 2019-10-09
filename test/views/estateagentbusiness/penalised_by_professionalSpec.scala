/*
 * Copyright 2019 HM Revenue & Customs
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
import  utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class penalised_by_professionalSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "penalised_by_professional view" must {

    "have the back link button" in new ViewFixture {
      val form2: ValidForm[ProfessionalBody] = Form2(ProfessionalBodyNo)

      def view = views.html.estateagentbusiness.penalised_by_professional(form2, edit = true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have correct title" in new ViewFixture {

      val form2: ValidForm[ProfessionalBody] = Form2(ProfessionalBodyNo)

      def view = views.html.estateagentbusiness.penalised_by_professional(form2, edit = true)

      doc.title must startWith(Messages("estateagentbusiness.penalisedbyprofessional.title") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ProfessionalBody] = Form2(ProfessionalBodyNo)

      def view = views.html.estateagentbusiness.penalised_by_professional(form2, edit = true)

      heading.html must be(Messages("estateagentbusiness.penalisedbyprofessional.title"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "penalised") -> Seq(ValidationError("not a message Key")),
          (Path \ "professionalBody") -> Seq(ValidationError("not another message key"))
        ))

      def view = views.html.estateagentbusiness.penalised_by_professional(form2, edit = true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("not another message key")

      doc.getElementById("penalised")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("professionalBody")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not another message key")
    }
  }
}