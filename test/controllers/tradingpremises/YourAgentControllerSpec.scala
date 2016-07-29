package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class YourAgentControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new YourAgentController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "YourAgentController" must {

    "use correct services" in new Fixture {
      YourAgentController.authConnector must be(AMLSAuthConnector)
      YourAgentController.dataCacheConnector must be(DataCacheConnector)
    }

    "on get display the blank trading premises your agent page" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(
        TradingPremises(None, None, None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("tradingpremises.youragent.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select(s"input[id=agentsRegisteredName]").`val`() must be(empty)

    }

    "on get display the your agent page with pre populated data" in new Fixture {

      val taxType: TaxType = TaxTypeSelfAssesment
      val businessStructure: BusinessStructure = LimitedLiabilityPartnership

      val yourAgent = YourAgent("Agents Name", taxType, businessStructure)

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(
        TradingPremises(None, Some(yourAgent), None)))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      contentAsString(result) must include(Messages("tradingpremises.youragent.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select(s"input[id=agentsRegisteredName]").`val`() must be("Agents Name")
    }

    "for an Invalid Request must give a Bad Request" in new Fixture {
      val invalidRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "",
        "taxType" -> "",
        "agentsBusinessStructure" -> ""
      )

      val RecordId = 1
      val result = controller.post(RecordId)(invalidRequest)
      status(result) must be(BAD_REQUEST)
    }

    "for a Valid Request with edit False must redirect to WhatDoesYourBusinessDoController " in new Fixture {

      val validRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "Agents Registered Name",
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )

      val taxType: TaxType = TaxTypeSelfAssesment
      val businessStructure: BusinessStructure = LimitedLiabilityPartnership

      val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(
        TradingPremises(None, Some(yourAgent), None)))))

      val RecordId = 1
      val result = controller.post(RecordId)(validRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(RecordId).url))

    }

    "for a Valid Request with edit true must redirect to Summary Controller" in new Fixture {

      val validRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "Agents Registered Name",
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )

      val taxType: TaxType = TaxTypeSelfAssesment
      val businessStructure: BusinessStructure = LimitedLiabilityPartnership

      val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(
        TradingPremises(None, Some(yourAgent), None)))))

      val RecordId = 1
      val result = controller.post(RecordId, true)(validRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.getIndividual(RecordId).url))

    }
  }
}
