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

import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.NewServiceInformationView

class NewServiceInformationViewSpec extends AmlsViewSpec with Matchers {

  lazy val new_service_information                               = app.injector.instanceOf[NewServiceInformationView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = new_service_information(Set(AccountancyServices), false, Set())
  }

  "NewServiceInformationView" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.newserviceinformation.title") + " - " + messages(
          "summary.updateservice"
        )
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.newserviceinformation.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "show the correct content" when {

      "only ASP is selected" in new ViewFixture {
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.text() must include(
          messages("businessmatching.updateservice.newserviceinformation.info.6", AccountancyServices.getMessage(true))
        )
      }

      "only TCSP is selected" in new ViewFixture {
        override def view = new_service_information(Set(TrustAndCompanyServices), false, Set())

        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.text() must include(
          messages(
            "businessmatching.updateservice.newserviceinformation.info.6",
            TrustAndCompanyServices.getMessage(true)
          )
        )
      }

      "only BPSP is selected" in new ViewFixture {
        override def view = new_service_information(Set(BillPaymentServices), false, Set(), true)

        doc.text() must include(
          messages("businessmatching.updateservice.newserviceinformation.info.6", BillPaymentServices.getMessage(true))
        )
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.7"))
      }

      "only MSB is selected with MT" in new ViewFixture {
        override def view = new_service_information(Set(MoneyServiceBusiness), false, Set(TransmittingMoney))

        doc.text() must include(
          messages("businessmatching.updateservice.newserviceinformation.info.1", MoneyServiceBusiness.getMessage(true))
        )
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.6.msb.single"))

      }

      "both ASP and TCSP are selected" in new ViewFixture {
        override def view =
          new_service_information(Set(TrustAndCompanyServices, AccountancyServices), false, Set(), false, true)

        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }

      "both TCSP and TDI are selected" in new ViewFixture {
        override def view =
          new_service_information(Set(TrustAndCompanyServices, TelephonePaymentService), false, Set(), true, true)

        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }

      "MSB, TCSP and TDI are selected" in new ViewFixture {
        override def view = new_service_information(
          Set(TrustAndCompanyServices, TelephonePaymentService, MoneyServiceBusiness),
          false,
          Set(TransmittingMoney),
          true,
          true
        )

        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.text() must include(messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }
    }

    "not show the return link" in new ViewFixture {
      doc.body().text() must not include messages("link.return.registration.progress")
    }

    behave like pageWithBackLink(new_service_information(Set(AccountancyServices), false, Set()))
  }
}
