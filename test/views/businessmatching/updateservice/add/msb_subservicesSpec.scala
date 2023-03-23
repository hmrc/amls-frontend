/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.businessmatching.MsbSubSectorsFormProvider
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BusinessMatchingMsbService, BusinessMatchingMsbServices}
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import org.jsoup.nodes.Element
import play.api.data.{Form, FormError}
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._

class msb_subservicesSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    lazy val msb_subservices = app.injector.instanceOf[msb_subservices]
    lazy val formProvider = app.injector.instanceOf[MsbSubSectorsFormProvider]
    implicit val requestWithToken = addTokenForView()

    def view = msb_subservices(formProvider(), edit = false)
  }

  "The msb_subservices view" must {

    "have correct title" in new ViewFixture {

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      override def view = msb_subservices(filledForm, edit = false)

      doc.title must startWith(Messages("businessmatching.updateservice.msb.services.title") + " - " + Messages("summary.updateservice"))
      heading.html must be(Messages("businessmatching.updateservice.msb.services.title"))
      subHeading.html must include(Messages("summary.updateservice"))
      doc.getElementById("back-link").isInstanceOf[Element] mustBe true

    }

    "show errors in the correct locations" in new ViewFixture {

      val messageKey = "error.required.msb.services"

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().withError(FormError("value", messageKey))

      override def view = msb_subservices(filledForm, edit = false)

      doc.getElementsByClass("govuk-list govuk-error-summary__list")
        .first.text() mustBe messages(messageKey)

      doc.getElementById("value-error").text() mustBe(s"Error: ${messages(messageKey)}")
    }

    "hide the return to progress link" in new ViewFixture {
      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      override def view = msb_subservices(filledForm, edit = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show the correct number of checkboxes when fxToggle is disabled" in new ViewFixture {
      override def view = msb_subservices(formProvider(), edit=false)
      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 4
    }

    "show the correct label for the checkboxes when fxToggle is disabled" in new ViewFixture {
      override def view = msb_subservices(formProvider(), edit = false)

      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      (0 until checkboxes.size()) foreach { i =>
        checkboxes.get(i).attr("value") mustEqual BusinessMatchingMsbServices.all(i).toString
      }
    }
  }
}