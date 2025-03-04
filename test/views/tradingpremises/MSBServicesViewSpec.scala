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

package views.tradingpremises

import forms.tradingpremises.MSBServicesFormProvider
import models.businessmatching.BusinessMatchingMsbService.TransmittingMoney
import models.tradingpremises.TradingPremisesMsbService
import models.tradingpremises.TradingPremisesMsbService._
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.Aliases
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.MSBServicesView

class MSBServicesViewSpec extends AmlsViewSpec with Matchers {

  lazy val msb_services: MSBServicesView = inject[MSBServicesView]
  lazy val fp: MSBServicesFormProvider   = inject[MSBServicesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val filteredModels: Seq[TradingPremisesMsbService] = Seq(
    ChequeCashingScrapMetal,
    ChequeCashingNotScrapMetal,
    CurrencyExchange,
    ForeignExchange
  )
  val checkboxes: Seq[Aliases.CheckboxItem]          = TradingPremisesMsbService.formValues(Some(filteredModels))

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "MSBServicesView" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.msb.services.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = msb_services(fp(), 1, false, false, checkboxes)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.msb.services.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      val checkbox = doc.select("input[type=checkbox]")
      checkbox.size mustBe 1
      checkbox.`val`() mustBe TransmittingMoney.toString
    }

    behave like pageWithErrors(
      msb_services(fp().withError("value", "error.required.tp.services"), 1, false, false, checkboxes),
      "value",
      "error.required.tp.services"
    )

    behave like pageWithBackLink(msb_services(fp(), 1, false, false, checkboxes))
  }
}
