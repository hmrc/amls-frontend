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
import models.registrationprogress.{Started, Completed, NotStarted, Section}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsUndefined, Json}
import uk.gov.hmrc.http.cache.client.CacheMap

class HvdSpec extends PlaySpec with MockitoSugar{

  val DefaultCashPayment = CashPaymentYes(new LocalDate(1956, 2, 15))
  private val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
  private val DefaultReceiveCashPayments = ReceiveCashPayments(Some(paymentMethods))

  val NewCashPayment = CashPaymentNo

  val completeModel = Hvd(cashPayment = Some(DefaultCashPayment),
    None,None,None,None,
    receiveCashPayments = Some(DefaultReceiveCashPayments),
    None,
    Some(DateOfChange(new LocalDate("2016-02-24"))))

  "hvd" must {

    val completeJson = Json.obj(
      "cashPayment" ->Json.obj(
        "acceptedAnyPayment" -> true,
        "paymentDate" -> new LocalDate(1956, 2, 15)
      ),
      "receiveCashPayments" -> Json.obj(
        "receivePayments" -> true,
        "paymentMethods" -> Json.obj(
          "courier" -> true,
          "direct" -> true,
          "other" -> true,
          "details" -> "foo")
      ),
      "dateOfChange" -> "2016-02-24",
      "hasChanged" -> false
    )

    "Serialise as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }
    "Deserialise as expected" in {
      completeJson.as[Hvd] must be(completeModel)
    }

    "Update how will you sell goods correctly" in {
      val sut = Hvd(cashPayment = Some(DefaultCashPayment), howWillYouSellGoods = Some(HowWillYouSellGoods(Seq(Retail))))
      val expectedModel = Hvd(
        cashPayment = Some(DefaultCashPayment),
        howWillYouSellGoods = Some(HowWillYouSellGoods(Seq(Wholesale))),
        hasChanged = true
      )
      sut.howWillYouSellGoods(HowWillYouSellGoods(Seq(Wholesale))) must be (expectedModel)
    }
  }

  "Hvd Serialisation" when {
    "recieveCashPayments is missing" must {
      "skip the field in the resulting Json" in {
        val res = Json.toJson(completeModel.copy(receiveCashPayments = None))
        res \ "receiveCashPayments" mustBe a[JsUndefined]
      }
    }
  }

  "Section"  must {
    "have a mongo key that" must {
      "be correctly set" in {
        Hvd.key must be("hvd")
      }
    }

    "have a section function that" must {

      implicit val cache = mock[CacheMap]

      "return a NotStarted Section when model is empty" in {

        val notStartedSection = Section("hvd", NotStarted, false,  controllers.hvd.routes.WhatYouNeedController.get())

        when(cache.getEntry[Hvd]("hvd")) thenReturn None

        Hvd.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Hvd]
        val completedSection = Section("hvd", Completed, false,  controllers.hvd.routes.SummaryController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(complete)

        Hvd.section must be(completedSection)
      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Hvd]
        val startedSection = Section("hvd", Started, false, controllers.hvd.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(incompleteTcsp)

        Hvd.section must be(startedSection)
      }
    }
  }
}
