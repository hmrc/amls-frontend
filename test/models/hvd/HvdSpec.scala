package models.hvd

import controllers.hvd.routes.javascript
import models.registrationprogress.{Started, Completed, NotStarted, Section}
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatest.Matchers._
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
    receiveCashPayments = Some(DefaultReceiveCashPayments))

  "hvd" must {

    val completeJson = Json.obj(
      "cashPayment" ->Json.obj(
        "acceptedAnyPayment" -> true,
        "paymentDate" ->  new LocalDate(1956, 2, 15)
      ),
      "receiveCashPayments" -> Json.obj(
        "receivePayments" -> true,
        "paymentMethods" -> Json.obj(
          "courier" -> true,
          "direct" -> true,
          "other" -> true,
          "details" -> "foo")
      ),
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
