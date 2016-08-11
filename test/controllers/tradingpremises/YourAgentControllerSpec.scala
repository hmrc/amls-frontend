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

  val RecordId1 = 1

  "YourAgentController" when {
    "get is called" must {
      "respond with OK" when {
        "there is no data, and display the blank 'your agent' page" in new Fixture {

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, None)))))

          val result = controller.get(RecordId1)(request)
          val document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          contentAsString(result) must include(Messages("tradingpremises.youragent.title"))
          document.select(s"input[id=agentsRegisteredName]").`val`() must be(empty)
        }

        "there is already data, and display the your agent page with pre populated data" in new Fixture {

          val taxType: TaxType = TaxTypeSelfAssesment
          val businessStructure: BusinessStructure = LimitedLiabilityPartnership
          val yourAgent = YourAgent("Agents Name", taxType, businessStructure)

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, Some(yourAgent), None)))))

          val result = controller.get(RecordId1)(request)
          val document = Jsoup.parse(contentAsString(result))

          status(result) must be(OK)
          contentAsString(result) must include(Messages("tradingpremises.youragent.title"))
          document.select(s"input[id=agentsRegisteredName]").`val`() must be("Agents Name")
        }
      }

      "respond with NOT_FOUND" when {
        "there is no data at all at that index" in new Fixture {
          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(None))

          val result = controller.get(RecordId1)(request)

          status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" must {

      "respond with BAD_REQUEST" when {


        "given an Invalid Request" in new Fixture {
          val invalidRequest = request.withFormUrlEncodedBody(
            "agentsRegisteredName" -> "",
            "taxType" -> "",
            "agentsBusinessStructure" -> ""
          )

          val result = controller.post(RecordId1, false)(invalidRequest)
          status(result) must be(BAD_REQUEST)
        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {
          val validRequest = request.withFormUrlEncodedBody(
            "agentsRegisteredName" -> "Agents Registered Name",
            "taxType" -> "01",
            "agentsBusinessStructure" -> "01"
          )

          val taxType: TaxType = TaxTypeSelfAssesment
          val businessStructure: BusinessStructure = LimitedLiabilityPartnership
          val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, Some(yourAgent), None)))))

          val result = controller.post(10, false)(validRequest)

          status(result) must be(NOT_FOUND)

        }
      }

      "respond with SEE_OTHER" when {
        "given a Valid Request with edit False, and redirect to WhatDoesYourBusinessDoController" in new Fixture {

          val validRequest = request.withFormUrlEncodedBody(
            "agentsRegisteredName" -> "Agents Registered Name",
            "taxType" -> "01",
            "agentsBusinessStructure" -> "01"
          )

          val taxType: TaxType = TaxTypeSelfAssesment
          val businessStructure: BusinessStructure = LimitedLiabilityPartnership
          val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, Some(yourAgent), None)))))

          val result = controller.post(RecordId1, false)(validRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(RecordId1).url))
        }

        "given a Valid Request with edit False when there is no data, and redirect to WhatDoesYourBusinessDoController" in new Fixture {

          val validRequest = request.withFormUrlEncodedBody(
            "agentsRegisteredName" -> "Agents Registered Name",
            "taxType" -> "01",
            "agentsBusinessStructure" -> "01"
          )

          val taxType: TaxType = TaxTypeSelfAssesment
          val businessStructure: BusinessStructure = LimitedLiabilityPartnership
          val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, None, None)))))

          val result = controller.post(RecordId1, false)(validRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(RecordId1).url))
        }

        "given a Valid Request with edit true, and redirect to Summary Controller" in new Fixture {

          val validRequest = request.withFormUrlEncodedBody(
            "agentsRegisteredName" -> "Agents Registered Name",
            "taxType" -> "01",
            "agentsBusinessStructure" -> "01"
          )

          val taxType: TaxType = TaxTypeSelfAssesment
          val businessStructure: BusinessStructure = LimitedLiabilityPartnership

          val yourAgent = YourAgent("Agents Registered Name", taxType, businessStructure)

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(None, None, Some(yourAgent), None)))))

          val result = controller.post(RecordId1, true)(validRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(
            Some(controllers.tradingpremises.routes.SummaryController.getIndividual(RecordId1).url))
        }
      }
    }
  }

  it must {
    "use correct services" in new Fixture {
      YourAgentController.authConnector must be(AMLSAuthConnector)
      YourAgentController.dataCacheConnector must be(DataCacheConnector)
    }
  }
}
