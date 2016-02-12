package controllers.tradingpremises

import connectors.DataCacheConnector
import models.businessmatching._
import models.tradingpremises._
import org.json.JSONObject
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.{when, verify}
import org.mockito.stubbing.Answer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import org.specs2.execute.Pending
import play.api.libs.json.Format
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class WhatDoesYourBusinessDoControllerSpec extends PlaySpec
      with OneServerPerSuite
      with MockitoSugar
      with ScalaFutures {

//  val yourAgent1 = YourAgent(AgentsRegisteredName("ABC Corporation"), TaxTypeCorporationTax, SoleProprietor)
  val yourAgent2 = YourAgent(AgentsRegisteredName("PQR Corporation"), TaxTypeCorporationTax, SoleProprietor)
//  val yourAgent3 = YourAgent(AgentsRegisteredName("XYZ Corporation"), TaxTypeCorporationTax, SoleProprietor)

  val whatDoesYourBusinessDoWithOneActivity = WhatDoesYourBusinessDo(Set(EstateAgentBusinessService))
  val tradingPremisesWithSingleBusinessActivity = TradingPremises(None, None, Some(whatDoesYourBusinessDoWithOneActivity) )
  val tradingPremisesWithAgentSet = TradingPremises(None, Some(yourAgent2))

  val activityData1:Set[BusinessActivity] = Set(EstateAgentBusinessService)
//  val activityData2:Set[BusinessActivity] = Set(HighValueDealing, MoneyServiceBusiness)
//  val activityData3:Set[BusinessActivity] = Set(TrustAndCompanyServices, TelephonePaymentService)

  val businessActivities1 = BusinessActivities(activityData1)
//  val businessActivities2 = BusinessActivities(activityData2)

  val businessMatching1 = BusinessMatching(Some(businessActivities1))

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new WhatDoesYourBusinessDoController {
      override val authConnector = self.authConnector
      override val dataCacheConnector = mock[DataCacheConnector]
    }
  }

  "WhatDoesYourBusinessDoController" when {
    "The business only engages in 1 activity" when {
      "Trading Premises data does not already exists in the cache" when {
        "get is called" should {
          "redirect to check your answers page" in new Fixture {
            when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](Matchers.eq(BusinessMatching.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))
            when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](Matchers.eq(TradingPremises.key))
              (any(), any(), any())).thenReturn(Future.successful(None))


            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result).getOrElse("FAILED") must endWith("trading-premises/summary")
          }

          "Automatically write the appropriate value to dataCache" in new Fixture {
            when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](Matchers.eq(BusinessMatching.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))
            when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](Matchers.eq(TradingPremises.key))
              (any(), any(), any())).thenReturn(Future.successful(None))

            whenReady(controller.get()(request)) { result  =>
              verify(controller.dataCacheConnector)
                .saveDataShortLivedCache[TradingPremises](Matchers.eq(TradingPremises.key), Matchers.eq(tradingPremisesWithSingleBusinessActivity))(any[AuthContext], any[HeaderCarrier], any[Format[TradingPremises]])
            }
          }
        }
      }

      "Trading Premises data already exists in the cache" when {
        "get is called" should {
          "redirect to check your answers page" in new Fixture {
            when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](Matchers.eq(BusinessMatching.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))
            when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](Matchers.eq(TradingPremises.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(tradingPremisesWithAgentSet)))

            val result = controller.get()(request)

            status(result) must be(SEE_OTHER)
            redirectLocation(result).getOrElse("FAILED") must endWith("trading-premises/summary")
          }

          "Automatically write the appropriate value to dataCache" in new Fixture {
            when(controller.dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](Matchers.eq(BusinessMatching.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(businessMatching1)))
            when(controller.dataCacheConnector.fetchDataShortLivedCache[TradingPremises](Matchers.eq(TradingPremises.key))
              (any(), any(), any())).thenReturn(Future.successful(Some(tradingPremisesWithAgentSet)))

            whenReady(controller.get()(request)) { result =>
              verify(controller.dataCacheConnector)
                .saveDataShortLivedCache[TradingPremises](
                          Matchers.eq(TradingPremises.key),
                          Matchers.eq(TradingPremises(None,Some(yourAgent2),Some(whatDoesYourBusinessDoWithOneActivity))))(any[AuthContext], any[HeaderCarrier], any[Format[TradingPremises]])
            }
          }
        }
      }

      "The business engages in multiple activities" when {
        "get is called" should {
          "display WhatDoesYourBusinessDo Page" in Pending
          "include only the appropriate options in the list" in Pending
        }
      }
    }

    "Post is called" in Pending
  }
}
