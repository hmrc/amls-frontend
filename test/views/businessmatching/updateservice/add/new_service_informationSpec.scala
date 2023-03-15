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

import models.businessmatching.BusinessActivity._
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._


class new_service_informationSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val new_service_information = app.injector.instanceOf[new_service_information]
    implicit val requestWithToken = addTokenForView()

    def view = new_service_information(Set(AccountancyServices.getMessage()), false, Set())
  }

  "The new_service_information view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("businessmatching.updateservice.newserviceinformation.title") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("businessmatching.updateservice.newserviceinformation.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "have the back link button" in new ViewFixture {
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "show the correct content" when {

      "only ASP is selected" in new ViewFixture {
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.6", AccountancyServices.getMessage().toLowerCase))
      }

      "only TCSP is selected" in new ViewFixture {
        override def view = new_service_information(Set(TrustAndCompanyServices.getMessage()), false, Set())

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.6", TrustAndCompanyServices.getMessage().toLowerCase))
      }

      "only BPSP is selected" in new ViewFixture {
        override def view = new_service_information(Set(BillPaymentServices.getMessage()), false, Set(), true)

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.6", BillPaymentServices.getMessage().toLowerCase))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.7"))
      }

      "only MSB is selected with MT" in new ViewFixture {
        override def view = new_service_information(Set(MoneyServiceBusiness.getMessage()), false, Set(TransmittingMoney))

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.1", MoneyServiceBusiness.getMessage().toLowerCase))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.6.msb.single"))

      }

      "both ASP and TCSP are selected" in new ViewFixture {
        override def view = new_service_information(Set(TrustAndCompanyServices.getMessage(), AccountancyServices.getMessage()), false, Set(), false, true)

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }

      "both TCSP and TDI are selected" in new ViewFixture {
        override def view = new_service_information(Set(TrustAndCompanyServices.getMessage(), TelephonePaymentService.getMessage()), false, Set(), true, true)

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }

      "MSB, TCSP and TDI are selected" in new ViewFixture {
        override def view = new_service_information(Set(TrustAndCompanyServices.getMessage(), TelephonePaymentService.getMessage(), MoneyServiceBusiness.getMessage()), false, Set(TransmittingMoney), true, true)

        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.2"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.supervision"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.3"))
        doc.getElementById("content").text() must include(Messages("businessmatching.updateservice.newserviceinformation.info.4"))
      }
    }

    "not show the return link" in new ViewFixture {
      doc.body().text() must not include Messages("link.return.registration.progress")
    }
  }
}
