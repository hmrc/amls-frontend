package controllers.tradingpremises

import java.util.UUID

import connectors.DataCacheConnector
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}
import models.businessmatching.{AccountancyServices, BillPaymentServices, BusinessMatching, EstateAgentBusinessService}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  implicit val request = FakeRequest
  val userId = s"user-${UUID.randomUUID()}"
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[CacheMap]

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val summaryController = new SummaryController {
      override val dataCacheConnector = mockDataCacheConnector
      override val authConnector = self.authConnector
    }
  }

  "SummaryController" must {

    "load the summary page when the model is present" in new Fixture {
      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      val model = TradingPremises()
      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.get()(request)
      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      val model = TradingPremises()
      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(None)
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "for an individual display the trading premises summary page for individual" in new Fixture {

      val model = TradingPremises()
      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
      val result = summaryController.getIndividual(1)(request)
      status(result) must be(OK)
    }


    "for an individual redirect to the trading premises summary summary if data is not present" in new Fixture {
      when(mockDataCacheConnector.fetch[Seq[TradingPremises]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = summaryController.getIndividual(1)(request)
      status(result) must be(NOT_FOUND)
    }

    "direct to your answers when the model is present" in new Fixture {
      val businessMatchingActivitiesAll = BusinessMatchingActivities(
        Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
      val model = TradingPremises()
      when(mockDataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
        .thenReturn(Future.successful(Some(mockCacheMap)))
      when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
        .thenReturn(Some(Seq(model)))
      when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
        .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

      val result = summaryController.answers()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("summary.youranswers.title"))
    }

  }

  "ModelHelpers" must {

    import controllers.tradingpremises.ModelHelpers._

    "return the correct removal url" when {

      "the trading premises is an Agent trading premises" in {

        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)))

        tradingPremises.removeUrl(1) must be(
          controllers.tradingpremises.routes.RemoveAgentPremisesReasonsController.get(1).url)

      }

      "the trading premises is not an Agent trading premises" in {
        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(false)))

        tradingPremises.removeUrl(1) must be(
          controllers.tradingpremises.routes.RemoveTradingPremisesController.get(1).url
        )
      }

    }

  }
}
