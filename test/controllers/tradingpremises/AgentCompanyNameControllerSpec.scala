package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{TaxType, AgentCompanyName, TradingPremises}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

class AgentCompanyNameControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new AgentCompanyNameController {
      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override val authConnector: AuthConnector = self.authConnector
    }
  }

  "AgentCompanyNameController" must {

    val emptyCache = CacheMap("", Map.empty)

    "display agent company name Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.title() must be (Messages("tradingpremises.agentcompanyname.title"))
      document.select("input[type=text]").`val`() must be(empty)
    }

    "display main Summary Page" in new Fixture {

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises(agentCompanyName = Some(AgentCompanyName("test")))))))

      val result = controller.get(1)(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))

      document.title() must be (Messages("tradingpremises.agentcompanyname.title"))
      document.select("input[type=text]").`val`() must be("test")
    }

    "post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentCompanyName" -> "text"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))
    }

    "post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentCompanyName" -> "text"
      )

      when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(TradingPremises()))))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1,true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.getIndividual(1).url))

    }

    "post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentCompanyName" -> "11111111111"*40
      )

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.invalid.tp.agent.registered.company.name"))

    }

    "respond with NOT_FOUND" when {
      "there is no data at all at that index" in new Fixture {
        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)

        status(result) must be(NOT_FOUND)
      }
    }



    "post with missing mandatory field" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "agentCompanyName" -> " "
      )

      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyCache))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("error.required.tp.agent.registered.company.name"))
    }
  }
}
