package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{Address, YourTradingPremises, TradingPremises}
import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.{MustMatchers, WordSpecLike}
import org.scalatestplus.play.OneAppPerSuite
import play.api.mvc.Call
import utils.AuthorisedFixture
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._

import scala.annotation.tailrec
import scala.concurrent.Future


class TradingPremisesAddControllerSpec extends WordSpecLike
  with MustMatchers with MockitoSugar with ScalaFutures with OneAppPerSuite with PropertyChecks {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new TradingPremisesAddController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    @tailrec
    final def buildTestSequence(requiredCount: Int, acc: Seq[TradingPremises] = Nil): Seq[TradingPremises] = {
      require(requiredCount >= 0, "cannot build a sequence with negative elements")
      if (requiredCount == acc.size) {
        acc
      } else {
        val tradingPremisesData = TradingPremises(
          Some(YourTradingPremises(
            "Trading Name",
            Address(
              "line 1", "line 2", Some("line 3"), Some("line 4"), "postcode"
            ),
            true,
            new LocalDate(10,10,10),
            false
          ))
        )

        buildTestSequence(requiredCount, acc :+ tradingPremisesData)
      }
    }

    def guidanceOptions(currentCount: Int) = Table(
      ("guidanceRequested", "expectedRedirect"),
      (true, controllers.tradingpremises.routes.WhatYouNeedController.get(currentCount + 1)),
      (false, controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(currentCount + 1, false))
    )
  }

  "TradingPremisesAddController" when {
    "get is called" should {
      "add empty trading premises and redirect to the correct page" in new Fixture {
        val min = 0
        val max = 25
        val requiredSuccess =10


        val zeroCase = Gen.const(0)
        val reasonableCounts = for (n <- Gen.choose(min, max)) yield n
        val partitions = Seq (zeroCase, reasonableCounts)

        forAll(reasonableCounts, minSuccessful(requiredSuccess)) { currentCount: Int =>
          forAll(guidanceOptions(currentCount)) { (guidanceRequested: Boolean, expectedRedirect: Call) =>
            val testSeq  = buildTestSequence(currentCount)
            println(s"currentCount = $currentCount")

            when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(testSeq)))

            val resultF = controller.get(guidanceRequested)(request)

            status(resultF) must be(SEE_OTHER)
            redirectLocation(resultF) must be(Some(expectedRedirect.url))

            verify(controller.dataCacheConnector)
              .save[Seq[TradingPremises]](meq(TradingPremises.key), meq(testSeq :+ TradingPremises()))(any(), any(), any())

            reset(controller.dataCacheConnector)
          }
        }
      }
    }
  }
}
