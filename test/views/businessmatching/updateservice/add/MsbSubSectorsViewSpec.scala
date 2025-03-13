/*
 * Copyright 2024 HM Revenue & Customs
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
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.businessmatching.{BusinessMatchingMsbService, BusinessMatchingMsbServices}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.MsbSubSectorsView

class MsbSubSectorsViewSpec extends AmlsViewSpec {

  lazy val msb_subservices                                       = inject[MsbSubSectorsView]
  lazy val formProvider                                          = inject[MsbSubSectorsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = msb_subservices(formProvider(), edit = false)
  }

  "MsbSubSectorsView" must {

    "have correct title" in new ViewFixture {

      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      override def view = msb_subservices(filledForm, edit = false)

      doc.title       must startWith(
        Messages("businessmatching.updateservice.msb.services.title") + " - " + Messages("summary.updateservice")
      )
      heading.html    must be(Messages("businessmatching.updateservice.msb.services.title"))
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "hide the return to progress link" in new ViewFixture {
      val filledForm: Form[Seq[BusinessMatchingMsbService]] = formProvider().fill(Seq(TransmittingMoney))

      override def view = msb_subservices(filledForm, edit = false)

      doc.body().text() must not include Messages("link.return.registration.progress")
    }

    "show the correct number of checkboxes when fxToggle is disabled" in new ViewFixture {
      override def view = msb_subservices(formProvider(), edit = false)
      doc.body().getElementsByAttributeValue("type", "checkbox").size() mustEqual 4
    }

    "show the correct label for the checkboxes when fxToggle is disabled" in new ViewFixture {
      override def view = msb_subservices(formProvider(), edit = false)

      val checkboxes = doc.body().getElementsByAttributeValue("type", "checkbox")
      (0 until checkboxes.size()) foreach { i =>
        BusinessMatchingMsbServices.all.map(_.toString) must contain(checkboxes.get(i).attr("value"))
      }
    }

    behave like pageWithErrors(
      msb_subservices(formProvider().withError("value", "error.required.msb.services"), false),
      "value",
      "error.required.msb.services"
    )

    behave like pageWithBackLink(msb_subservices(formProvider(), false))
  }
}
