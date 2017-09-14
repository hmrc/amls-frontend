/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsUndefined, Json}
import uk.gov.hmrc.http.cache.client.CacheMap
import org.scalatest.MustMatchers

sealed trait HvdTestFixture {
  val DefaultCashPayment = CashPaymentYes(new LocalDate(1956, 2, 15))
  private val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
  private val DefaultReceiveCashPayments = ReceiveCashPayments(Some(paymentMethods))

  val NewCashPayment = CashPaymentNo

  val completeModel = Hvd(cashPayment = Some(DefaultCashPayment),
    Some(Products(Set(Cars))),
    None,
    Some(HowWillYouSellGoods(Seq(Retail))),
    Some(PercentageOfCashPaymentOver15000.First),
    receiveCashPayments = Some(DefaultReceiveCashPayments),
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

    "Serialise as expected" in new HvdTestFixture {
      Json.toJson(completeModel) must be(completeJson)
    }
    "Deserialise as expected" in new HvdTestFixture {
      completeJson.as[Hvd] must be(completeModel)
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

class HvdWithHasAcceptedSpec extends PlaySpec with MustMatchers with OneAppPerSuite with MockitoSugar {
  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.has-accepted" -> true)
    .build()

  "HVD" when {
    "isComplete is called" must {
      "return true if the model has been accepted" in new HvdTestFixture {
        completeModel.copy(hasAccepted = true).isComplete mustBe true
      }

      "return false if the model has not been accepted" in new HvdTestFixture {
        completeModel.copy(hasAccepted = false).isComplete mustBe false
      }
    }

    "a field is changed" must {

      val tests = Seq[(Hvd => Hvd, String)](
        (_.cashPayment(CashPaymentNo), "cashPayment"),
        (_.products(Products(Set(ScrapMetals))), "products"),
        (_.receiveCashPayments(ReceiveCashPayments(Some(PaymentMethods(false, false, None)))), "receiveCashPayments"),
        (_.exciseGoods(ExciseGoods(false)), "exciseGoods"),
        (_.linkedCashPayment(LinkedCashPayments(true)), "linkedCashPayments"),
        (_.howWillYouSellGoods(HowWillYouSellGoods(Seq(Wholesale))), "howWillYouSellGoods"),
        (_.percentageOfCashPaymentOver15000(PercentageOfCashPaymentOver15000.Second), "percentageOfCashPaymentOver15000")
      )

      "reset hasAccepted back to false" when {
        tests foreach { test =>
          s"${test._2} is changed" in new HvdTestFixture {
            test._1(completeModel.copy(hasAccepted = true)).hasAccepted mustBe false
          }
        }
      }
    }
  }
}
