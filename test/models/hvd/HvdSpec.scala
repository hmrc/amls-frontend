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

package models.hvd

import models.DateOfChange
import models.registrationprogress.{Completed, NotStarted, Section, Started}
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsUndefined, Json}
import uk.gov.hmrc.http.cache.client.CacheMap

sealed trait HvdTestFixture {
  val DefaultCashPayment = CashPayment(CashPaymentOverTenThousandEuros(true), Some(CashPaymentFirstDate(new LocalDate(1956, 2, 15))))
  private val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))

  val NewCashPayment = CashPayment(CashPaymentOverTenThousandEuros(false), None)

  val completeModel = Hvd(
    cashPayment = Some(DefaultCashPayment),
    Some(Products(Set(Cars))),
    None,
    Some(HowWillYouSellGoods(Seq(Retail))),
    Some(PercentageOfCashPaymentOver15000.First),
    Some(true),
    Some(paymentMethods),
    Some(LinkedCashPayments(false)),
    Some(DateOfChange(new LocalDate("2016-02-24"))))
}

class HvdSpec extends PlaySpec with MockitoSugar {

  "hvd" must {

    val completeJson = Json.obj(
      "cashPayment" -> Json.obj(
        "acceptedAnyPayment" -> true,
        "paymentDate" -> new LocalDate(1956, 2, 15)
      ),
      "products" -> Json.obj(
        "products" -> Seq("04")
      ),
      "howWillYouSellGoods" -> Json.obj(
        "salesChannels" -> Seq("Retail")
      ),
      "percentageOfCashPaymentOver15000" -> Json.obj(
        "percentage" -> "01"
      ),
      "receiveCashPayments" -> true,
      "cashPaymentMethods" -> Json.obj(
        "courier" -> true,
        "direct" -> true,
        "other" -> true,
        "details" -> "foo"),
      "linkedCashPayment" -> Json.obj(
        "linkedCashPayments" -> false
      ),
      "dateOfChange" -> "2016-02-24",
      "hasChanged" -> false,
      "hasAccepted" -> false
    )

    "Serialise" in new HvdTestFixture {
      Json.toJson(completeModel) must be(completeJson)
    }
    "Deserialise" when {
      "current format json" in new HvdTestFixture {
        completeJson.as[Hvd] must be(completeModel)
      }
      "old format json" when {
        "receiveCashPayments is in old format json" in new HvdTestFixture {

          val completeJson = Json.obj(
            "cashPayment" -> Json.obj(
              "acceptedAnyPayment" -> true,
              "paymentDate" -> new LocalDate(1956, 2, 15)
            ),
            "products" -> Json.obj(
              "products" -> Seq("04")
            ),
            "howWillYouSellGoods" -> Json.obj(
              "salesChannels" -> Seq("Retail")
            ),
            "percentageOfCashPaymentOver15000" -> Json.obj(
              "percentage" -> "01"
            ),
            "receiveCashPayments" -> Json.obj(
              "receivePayments" -> true,
              "paymentMethods" -> Json.obj(
                "courier" -> true,
                "direct" -> true,
                "other" -> true,
                "details" -> "foo")
            ),
            "linkedCashPayment" -> Json.obj(
              "linkedCashPayments" -> false
            ),
            "dateOfChange" -> "2016-02-24",
            "hasChanged" -> false,
            "hasAccepted" -> false
          )

          completeJson.as[Hvd] must be(completeModel)

        }
      }
    }

    "Update how will you sell goods correctly" in new HvdTestFixture {
      val sut = Hvd(cashPayment = Some(DefaultCashPayment), howWillYouSellGoods = Some(HowWillYouSellGoods(Seq(Retail))))
      val expectedModel = Hvd(
        cashPayment = Some(DefaultCashPayment),
        howWillYouSellGoods = Some(HowWillYouSellGoods(Seq(Wholesale))),
        hasChanged = true
      )
      sut.howWillYouSellGoods(HowWillYouSellGoods(Seq(Wholesale))) must be(expectedModel)
    }
  }

  "Hvd Serialisation" when {
    "recieveCashPayments is missing" must {
      "skip the field in the resulting Json" in new HvdTestFixture {
        val res = Json.toJson(completeModel.copy(receiveCashPayments = None))
        res \ "receiveCashPayments" mustBe a[JsUndefined]
      }
    }
  }

  "Section" must {
    "have a mongo key that" must {
      "be correctly set" in {
        Hvd.key must be("hvd")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in new HvdTestFixture {

        val notStartedSection = Section("hvd", NotStarted, false, controllers.hvd.routes.WhatYouNeedController.get())

        when(cache.getEntry[Hvd]("hvd")) thenReturn None

        Hvd.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in new HvdTestFixture {

        val complete = mock[Hvd]
        val completedSection = Section("hvd", Completed, false, controllers.hvd.routes.SummaryController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(complete)

        Hvd.section must be(completedSection)
      }

      "return a Started Section when model is incomplete" in new HvdTestFixture {

        val incompleteTcsp = mock[Hvd]
        val startedSection = Section("hvd", Started, false, controllers.hvd.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(incompleteTcsp)

        Hvd.section must be(startedSection)
      }
    }
  }
}