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

package views.confirmation

import generators.PaymentGenerator
import models.confirmation.{BreakdownRow, Currency}
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class BreakdownSpec extends AmlsSpec with PaymentGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val continueHref = "http://google.co.uk"

    def getRowDataElements(index: Int) = {
      doc.getElementsByTag("tbody").first()
        .getElementsByTag("tr").get(index)
        .getElementsByTag("td")
    }

    val responsiblePeopleWithoutFitAndProper: BreakdownRow =
      BreakdownRow("confirmation.responsiblepeople", 5, Currency(50), Currency(150))

    val responsiblePeopleWithFitAndProper: BreakdownRow =
      BreakdownRow("confirmation.responsiblepeople.fp.passed", 6, Currency(60), Currency(160))

    val tradingPremisesZero: BreakdownRow =
      BreakdownRow("confirmation.tradingpremises.zero", 4, Currency(40), Currency(140))

    val tradingPremisesHalf: BreakdownRow =
      BreakdownRow("confirmation.tradingpremises.half", 3, Currency(30), Currency(130))

    val tradingPremisesFull: BreakdownRow =
      BreakdownRow("confirmation.tradingpremises", 2, Currency(20), Currency(120))

    val totalSubmissionFee: BreakdownRow =
      BreakdownRow("confirmation.submission", 1, Currency(10), Currency(110))

    val breakdownRows: Seq[BreakdownRow] = Seq(
      totalSubmissionFee,
      tradingPremisesFull,
      tradingPremisesHalf,
      tradingPremisesZero,
      responsiblePeopleWithoutFitAndProper,
      responsiblePeopleWithFitAndProper
    )

    override def view = views.html.confirmation.breakdown(
      Currency(1234),
      breakdownRows,
      continueHref
    )
  }

  trait totalSubmissionFeeViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(totalSubmissionFee))
  }

  trait TradingPremisesFullViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(tradingPremisesFull))
  }

  trait TradingPremisesHalfViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(tradingPremisesHalf))
  }

  trait TradingPremisesZeroViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(tradingPremisesZero))
  }

  trait ResponsiblePeopleWithoutFitAndProperViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(responsiblePeopleWithoutFitAndProper))
  }

  trait ResponsiblePeopleWithFitAndProperViewFixture extends ViewFixture {
    val rowDataElements = getRowDataElements(breakdownRows.indexOf(responsiblePeopleWithFitAndProper))

  }

  "The breakdown view" must {
    "show the breakdown row table quantity heading" in new ViewFixture {
      doc.getElementsByTag("th").get(1).text mustBe Messages("confirmation.quantity")
    }

    "show the breakdown row table fee per item heading" in new ViewFixture {
      doc.getElementsByTag("th").get(2).text mustBe Messages("confirmation.feeperitem")
    }

    "show the breakdown row table total heading" in new ViewFixture {
      doc.getElementsByTag("th").get(3).text mustBe Messages("confirmation.totalfee")
    }
  }

  "Responsible People without F & P row" must {
    "show the correct title" in new ResponsiblePeopleWithoutFitAndProperViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.responsiblepeople")
    }

    "show the correct quantity" in new ResponsiblePeopleWithoutFitAndProperViewFixture {
      rowDataElements.get(1).text mustEqual "5"
    }

    "show the correct fee per item" in new ResponsiblePeopleWithoutFitAndProperViewFixture {
      rowDataElements.get(2).text mustEqual "£50.00"
    }

    "show the correct total" in new ResponsiblePeopleWithoutFitAndProperViewFixture {
      rowDataElements.get(3).text mustEqual "£150.00"
    }
  }

  "Responsible People with F & P row" must {
    "show the correct title" in new ResponsiblePeopleWithFitAndProperViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.responsiblepeople.fp.passed")
    }

    "show the correct quantity" in new ResponsiblePeopleWithFitAndProperViewFixture {
      rowDataElements.get(1).text mustEqual "6"
    }

    "show the correct fee per item" in new ResponsiblePeopleWithFitAndProperViewFixture {
      rowDataElements.get(2).text mustEqual "£60.00"
    }

    "show the correct total" in new ResponsiblePeopleWithFitAndProperViewFixture {
      rowDataElements.get(3).text mustEqual "£160.00"
    }
  }

  "Full year Trading Premises Row" must {
    "show the correct title" in new TradingPremisesFullViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.tradingpremises")
    }

    "show the correct quantity" in new TradingPremisesFullViewFixture {
      rowDataElements.get(1).text mustEqual "2"
    }

    "show the correct fee per item" in new TradingPremisesFullViewFixture {
      rowDataElements.get(2).text mustEqual "£20.00"
    }

    "show the correct total" in new TradingPremisesFullViewFixture {
      rowDataElements.get(3).text mustEqual "£120.00"
    }
  }

  "Half year Trading Premises row" must {
    "show the correct title" in new TradingPremisesHalfViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.tradingpremises.half")
    }

    "show the correct quantity" in new TradingPremisesHalfViewFixture {
      rowDataElements.get(1).text mustEqual "3"
    }

    "show the correct fee per item" in new TradingPremisesHalfViewFixture {
      rowDataElements.get(2).text mustEqual "£30.00"
    }

    "show the correct total" in new TradingPremisesHalfViewFixture {
      rowDataElements.get(3).text mustEqual "£130.00"
    }
  }

  "No year Trading Premises row" must {
    "show the correct title" in new TradingPremisesZeroViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.tradingpremises.zero")
    }

    "show the correct quantity" in new TradingPremisesZeroViewFixture {
      rowDataElements.get(1).text mustEqual "4"
    }

    "show the correct fee per item" in new TradingPremisesZeroViewFixture {
      rowDataElements.get(2).text mustEqual "£40.00"
    }

    "show the correct total" in new TradingPremisesZeroViewFixture {
      rowDataElements.get(3).text mustEqual "£140.00"
    }
  }

  "Total row" must {
    "show the correct title" in new totalSubmissionFeeViewFixture {
      rowDataElements.get(0).text mustEqual Messages("confirmation.submission")
    }

    "not show correct quantity" in new totalSubmissionFeeViewFixture {
      rowDataElements.get(1).text mustEqual "1"
    }

    "not show correct fee per item" in new totalSubmissionFeeViewFixture {
      rowDataElements.get(2).text mustEqual "£10.00"
    }

    "show the correct total" in new totalSubmissionFeeViewFixture {
      rowDataElements.get(3).text mustEqual "£110.00"
    }
  }
}