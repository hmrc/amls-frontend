package controllers.tradingpremises

import connectors.DataCacheConnector
import models.TradingPremisesSection
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture
import models.businessmatching.{BusinessActivities => BusinessMatchingActivities, _}

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

  val mockCacheMap = mock[CacheMap]

  "RegisteringAgentPremisesController" must {

    "Get Option:" must {

      "load the Register Agent Premises page" in new Fixture {

        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
        val model = TradingPremises()
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.agent.premises.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        htmlValue.title mustBe title
      }

      "load Yes when save4later returns true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )
        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("agentPremises-true").attr("checked") mustBe "checked"

      }
      "load No when save4later returns false" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(false)
          )
        )
        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(Some(mockCacheMap)))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))
        htmlValue.getElementById("agentPremises-false").attr("checked") mustBe "checked"

      }

      "respond with NOT_FOUND when there is no data at all at the given index" in new Fixture {
        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )
        val businessMatchingActivitiesAll = BusinessMatchingActivities(
          Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness))
        when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
          .thenReturn(Future.successful(None))
        when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
          .thenReturn(Some(Seq(model)))
        when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
          .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

        val result = controller.get(1)(request)
        status(result) must be(NOT_FOUND)
      }

    }

    "Post" must {

      "on post invalid data show error" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody()
        when(controller.dataCacheConnector.fetch[RegisteringAgentPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post(1)(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("tradingpremises.agent.premises.title"))
        contentAsString(result) must include(Messages("err.summary"))
      }

      "redirect to the Trading Premises details page on submitting false and edit true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1,edit = true)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.SummaryController.getIndividual(1).url)
      }

      "redirect to the Trading Premises details page on submitting false and edit false" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1,edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhereAreTradingPremisesController.get(1).url)
      }
      "redirect to the 'what is your agent's business structure?' page on submitting true" in new Fixture {

        val model = TradingPremises(
          registeringAgentPremises = Some(
            RegisteringAgentPremises(true)
          )
        )

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "true"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(Seq(TradingPremises()))))

        when(controller.dataCacheConnector.save[Seq[TradingPremises]](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(new CacheMap("", Map.empty)))

        val result = controller.post(1,edit = false)(newRequest)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.BusinessStructureController.get(1,false).url)
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "agentPremises" -> "true"
          )

          when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(Seq(TradingPremises(Some(RegisteringAgentPremises(true)), None, None, None)))))

          val result = controller.post(10, false)(newRequest)

          status(result) must be(NOT_FOUND)

        }
      }

      "redirect to Trading Premises Details form" when {
        "MSB is not found in business matching" in new Fixture {
          val model = TradingPremises(
            registeringAgentPremises = Some(
              RegisteringAgentPremises(true)
            )
          )
          val businessMatchingActivitiesAll = BusinessMatchingActivities(
            Set(AccountancyServices, BillPaymentServices, EstateAgentBusinessService))
          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))
          when(mockCacheMap.getEntry[Seq[TradingPremises]](any())(any()))
            .thenReturn(Some(Seq(model)))
          when(mockCacheMap.getEntry[BusinessMatching](BusinessMatching.key))
            .thenReturn(Some(BusinessMatching(None, Some(businessMatchingActivitiesAll))))

          val result = controller.get(1)(request)
          status(result) must be(SEE_OTHER)

          redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1).url))

        }
      }

      "set the hasChanged flag to true" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "agentPremises" -> "false"
        )

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse))))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.WhereAreTradingPremisesController.get(1, false).url))

        verify(controller.dataCacheConnector).save[Seq[TradingPremises]](
          any(),
          meq(Seq(TradingPremisesSection.tradingPremisesWithHasChangedFalse.copy(
            hasChanged = true,
            registeringAgentPremises = Some(RegisteringAgentPremises(false)),
            agentName=None,
            businessStructure=None,
            agentCompanyDetails=None,
            agentPartnership=None
          ))))(any(), any(), any())
      }

    }
  }
}
