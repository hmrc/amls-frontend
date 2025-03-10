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

package views.hvd

import models.hvd.PercentageOfCashPaymentOver15000.Third
import models.hvd.Products._
import models.hvd._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import utils.AmlsSummaryViewSpec
import utils.hvd.CheckYourAnswersHelper
import views.Fixture
import views.html.hvd.CheckYourAnswersView

import java.time.LocalDate
import scala.jdk.CollectionConverters._

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with TableDrivenPropertyChecks with Injecting {

  lazy val cyaView   = inject[CheckYourAnswersView]
  lazy val cyaHelper = inject[CheckYourAnswersHelper]

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())
  }

  "CheckYourAnswersView view" must {
    "have correct title" in new ViewFixture {
      def view = cyaView(cyaHelper.getSummaryList(Hvd()))

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {
      def view = cyaView(cyaHelper.getSummaryList(Hvd()))

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.hvd"))
    }

    "include the provided data" in new ViewFixture {

      val list = cyaHelper.getSummaryList(
        Hvd(
          cashPayment = Some(
            CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(LocalDate.of(2012, 6, 20))))
          ),
          products = Some(
            Products(
              Set(
                Alcohol,
                Tobacco,
                Antiques,
                Cars,
                OtherMotorVehicles,
                Caravans,
                Jewellery,
                Gold,
                ScrapMetals,
                MobilePhones,
                Clothing,
                Other("Other Product")
              )
            )
          ),
          exciseGoods = Some(ExciseGoods(true)),
          howWillYouSellGoods = Some(HowWillYouSellGoods(SalesChannel.all.toSet)),
          percentageOfCashPaymentOver15000 = Some(Third),
          receiveCashPayments = Some(true),
          cashPaymentMethods = Some(PaymentMethods(true, true, Some("Other payment method"))),
          linkedCashPayment = Some(LinkedCashPayments(true))
        )
      )

      def view = cyaView(list)

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = list.rows.find(_.key.content.asHtml.body == key.text()).value

          maybeRow.key.content.asHtml.body must include(key.text())

          val valueText = maybeRow.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
    }
  }
}
