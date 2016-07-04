package models.hvd

import models.registrationprogress.{Started, Completed, NotStarted, Section}
import org.joda.time.LocalDate
import org.scalatest.matchers.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

class HvdSpec extends PlaySpec with MockitoSugar{

  // scalastyle:off
  val DefaultCashPayment = CashPaymentYes(new LocalDate(1956, 2, 15))

  val NewCashPayment = CashPaymentNo

  "hvd" must {

    val completeJson = Json.obj(
      "cashPayment" ->Json.obj(
      "acceptedAnyPayment" -> true,
      "paymentDate" ->  new LocalDate(1956, 2, 15)))

    val completeModel = Hvd(cashPayment = Some(DefaultCashPayment))


    "Serialise as expected" in {
      Json.toJson(completeModel) must be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[Hvd] must be(completeModel)
    }

    "Update how will you sell goods correctly" in {
      val sut = Hvd(Some(DefaultCashPayment), Some(HowWillYouSellGoods(Seq(Retail))))

      sut.howWillYouSellGoods(HowWillYouSellGoods(Seq(Wholesale))) must be (Hvd(Some(DefaultCashPayment),
                                                                                  Some(HowWillYouSellGoods(Seq(Wholesale)))))
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

        val notStartedSection = Section("hvd", NotStarted, controllers.hvd.routes.WhatYouNeedController.get())

        when(cache.getEntry[Hvd]("hvd")) thenReturn None

        Hvd.section must be(notStartedSection)

      }

      "return a Completed Section when model is complete" in {

        val complete = mock[Hvd]
        val completedSection = Section("hvd", Completed, controllers.hvd.routes.SummaryController.get())

        when(complete.isComplete) thenReturn true
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(complete)

        Hvd.section must be(completedSection)

      }

      "return a Started Section when model is incomplete" in {

        val incompleteTcsp = mock[Hvd]
        val startedSection = Section("hvd", Started, controllers.hvd.routes.WhatYouNeedController.get())

        when(incompleteTcsp.isComplete) thenReturn false
        when(cache.getEntry[Hvd]("hvd")) thenReturn Some(incompleteTcsp)

        Hvd.section must be(startedSection)

      }
    }
  }
}
