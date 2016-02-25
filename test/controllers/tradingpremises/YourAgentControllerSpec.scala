package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching._
import models.tradingpremises._
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class YourAgentControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new YourAgentController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "YourAgentController" must {
    val yourAgent1 = YourAgent(AgentsRegisteredName("ABC Corporation"), TaxTypeCorporationTax, SoleProprietor)
    val yourAgent2 = YourAgent(AgentsRegisteredName("PQR Corporation"), TaxTypeCorporationTax, SoleProprietor)
    val yourAgent3 = YourAgent(AgentsRegisteredName("XYZ Corporation"), TaxTypeCorporationTax, SoleProprietor)

    val tradingPremises1 = TradingPremises(None, Some(yourAgent1))
    val tradingPremises2 = TradingPremises(None, Some(yourAgent2))


    "on get display who is your agent page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("   ")
    }

    "on get() display the who is your agent page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(tradingPremises1)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
    }

    "on post with valid data" in new Fixture {

      val agentName = "XYZ"

      val newRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> agentName,
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )
      val tradingPremisesWithData = TradingPremises(yourTradingPremises = None, yourAgent = Some(yourAgent1))

      val cacheMap: CacheMap = CacheMap("data", Map(
        TradingPremises.key -> Json.toJson(tradingPremisesWithData)
      ))

      when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext])).thenReturn(Future.successful(Some(cacheMap)))


      when(controller.dataCacheConnector.saveDataShortLivedCache[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {
      val agentName = "XYZ"
      val newRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> agentName,
        "taxType" -> "09",
        "businessStructure" -> "07"
      )
      when(controller.dataCacheConnector.saveDataShortLivedCache[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
    }

    // to be valid after summary edit page is ready
    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "XYZ",
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )

      val tradingPremisesWithData = TradingPremises(yourTradingPremises = None, yourAgent = Some(yourAgent2))

      val cacheMap: CacheMap = CacheMap("data", Map(
        TradingPremises.key -> Json.toJson(tradingPremisesWithData)
      ))

      when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))

      when(controller.dataCacheConnector.saveDataShortLivedCache[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(0, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))
    }


    "on post when only One Business Activity is selected then go to the Summary Page " in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "XYZ",
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )

      val singleBusinessActivity = BusinessMatching(Some(BusinessActivities(Set(AccountancyServices))))

      val tradingPremisesWithData = TradingPremises(yourTradingPremises = None, yourAgent = Some(yourAgent2))
      val cacheMap: CacheMap = CacheMap("data", Map(
        TradingPremises.key -> Json.toJson(tradingPremisesWithData),
        BusinessMatching.key -> Json.toJson(singleBusinessActivity)
      ))

      when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))
      when(controller.dataCacheConnector.saveDataShortLivedCache[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(0, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))
    }


    "on post when MORE than One Business Activity is selected then go to the Trading Activity" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "agentsRegisteredName" -> "XYZ",
        "taxType" -> "01",
        "agentsBusinessStructure" -> "01"
      )

      val multipleBusinessActivity = BusinessMatching(Some(
        BusinessActivities(Set(
          AccountancyServices,
          BillPaymentServices,
          EstateAgentBusinessService))))

      val tradingPremisesWithData = TradingPremises(yourTradingPremises = None, yourAgent = Some(yourAgent2))
      val cacheMap: CacheMap = CacheMap("data", Map(
        TradingPremises.key -> Json.toJson(tradingPremisesWithData),
        BusinessMatching.key -> Json.toJson(multipleBusinessActivity)
      ))

      when(controller.dataCacheConnector.fetchAll(any(), any())).thenReturn(Future.successful(Some(cacheMap)))
      when(controller.dataCacheConnector.saveDataShortLivedCache[TradingPremises](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.post(0, true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(0, false).url))
    }
  }
}
