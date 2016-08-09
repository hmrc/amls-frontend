package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
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

class RegisteringAgentPremisesControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new RegisteringAgentPremisesController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisteringAgentPremisesController" must {

    "Get Option:" must {

      "load the Register Agent Premises page" in new Fixture {

        when(controller.dataCacheConnector.fetch[RegisteringAgentPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.title mustBe Messages("tradingpremises.agent.premises.title")
      }

      "load Yes when accountant For AMLS Regulations from save4later returns True" in new Fixture {

        val accountantForAMLSRegulations = Some(RegisteringAgentPremises(true))
        val activities = TradingPremises()

        when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-true").attr("checked") mustBe "checked"

      }

      "load No when accountant For AMLS Regulations from save4later returns No" in new Fixture {

        val accountantForAMLSRegulations = Some(RegisteringAgentPremises(false))
        val activities = TradingPremises()

        when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(activities)))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("accountantForAMLSRegulations-false").attr("checked") mustBe "checked"

      }
    }

    "Post" must {

      "on post invalid data show error" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody()
        when(controller.dataCacheConnector.fetch[RegisteringAgentPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("tradingpremises.agent.premises.heading"))

      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "WhatYouNeedController" -> ""
        )
        when(controller.dataCacheConnector.fetch[RegisteringAgentPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("err.summary"))

      }
    }
  }
}
