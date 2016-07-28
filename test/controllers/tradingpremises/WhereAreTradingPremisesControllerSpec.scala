package controllers.tradingpremises


import connectors.DataCacheConnector
import models.tradingpremises.{Address, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future


class WhereAreTradingPremisesControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  val mockDataCacheConnector = mock[DataCacheConnector]

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhereAreTradingPremisesController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)
  val fields = Array[String]("tradingName", "addressLine1", "addressLine2", "postcode")
  val RecordId1 = 1


  "WhereAreTradingPremisesController" when {

    "get is called" must {
      "respond with OK and show the form with data when there is data" in new Fixture {

        val address = Address("addressLine1", "addressLine2", None, None, "NE98 1ZZ")
        val yourTradingPremises = YourTradingPremises(tradingName = "trading Name", address, true, LocalDate.now(), true)
        val tradingPremises = TradingPremises(Some(yourTradingPremises), None, None)

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(tradingPremises))))

        val result = controller.get(RecordId1, true)(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
        for (field <- fields)
          document.select(s"input[id=$field]").`val`() must not be empty
      }

      "respond with OK and show the empty form when there is no data" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        val result = controller.get(RecordId1, false)(request)
        val document = Jsoup.parse(contentAsString(result))

        status(result) must be(OK)
        contentAsString(result) must include(Messages("tradingpremises.yourtradingpremises.title"))
        for (field <- fields)
          document.select(s"input[id=$field]").`val`() must be(empty)

      }

      "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {

        when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get(RecordId1, false)(request)

        status(result) must be(NOT_FOUND)
      }
    }




    "post with valid data For OWNER redirect to What does your business do" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "NE98 1ZZ",
        "isOwner" -> "true",
        "startDate.day" -> "01",
        "startDate.month" -> "02",
        "startDate.year" -> "2010",
        "isResidential" -> "true"
      )

      when(controller.dataCacheConnector.fetch[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(any())(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(0).url))
    }

    "post with valid data when NOT OWNER redirect to your agent" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "NE98 1ZZ",
        "isOwner" -> "false",
        "startDate.day" -> "01",
        "startDate.month" -> "02",
        "startDate.year" -> "2010",
        "isResidential" -> "true"
      )

      when(controller.dataCacheConnector.fetch[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(any())(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.YourAgentController.get(0).url))
    }

    "post with edit mode with valid data and is OWNER then redirect to Summary Controller" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "tradingName" -> "Trading Name",
        "addressLine1" -> "Address 1",
        "addressLine2" -> "Address 2",
        "postcode" -> "NE98 1ZZ",
        "isOwner" -> "true",
        "startDate.day" -> "01",
        "startDate.month" -> "02",
        "startDate.year" -> "2010",
        "isResidential" -> "true"
      )

      when(controller.dataCacheConnector.fetch[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val RecordId = 1

      val result = controller.post(RecordId, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(
        Some(controllers.tradingpremises.routes.SummaryController.getIndividual(RecordId).url))
    }
  }

}
