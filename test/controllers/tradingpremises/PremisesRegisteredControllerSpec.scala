package controllers.tradingpremises

import connectors.DataCacheConnector
import models.responsiblepeople.ResponsiblePeople
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
import utils.{StatusConstants, AuthorisedFixture}

import scala.concurrent.Future

class PremisesRegisteredControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new PremisesRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PremisesRegisteredController" must {

    "Get Option:" must {

      "load the Premises Registered page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        val htmlValue = Jsoup.parse(contentAsString(result))

        val title = s"${Messages("tradingpremises.premises.registered.title")} - ${Messages("summary.tradingpremises")} - ${Messages("title.amls")} - ${Messages("title.gov")}"

        htmlValue.title mustBe title
      }

      "load the Premises Registered page1" in new Fixture {
        val ytp = YourTradingPremises("foo", Address("1", "2", None, None, "asdfasdf"),
          true, new LocalDate(1990, 2, 24))

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(None,Some(ytp)), TradingPremises(registeringAgentPremises = None,
            yourTradingPremises = Some(ytp), status =  Some(StatusConstants.Deleted))))))

        val result = controller.get(1)(request)
        status(result) must be(OK)

        contentAsString(result) must include(Messages("tradingpremises.have.registered.premises.text", 1))
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes'" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("registerAnotherPremises" -> "true")

        when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tradingpremises.routes.TradingPremisesAddController.get(false).url))
      }

      "successfully redirect to the page on selection of 'no'" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody("registerAnotherPremises" -> "false")

        when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.save[TradingPremises](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post(1)(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.tradingpremises.routes.SummaryController.get().url))
      }
    }

    "on post invalid data show error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody()
      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("tradingpremises.want.to.register.another.premises"))

    }

    "on post with invalid data show error" in new Fixture {
      val newRequest = request.withFormUrlEncodedBody(
        "registerAnotherPremises" -> ""
      )
      when(controller.dataCacheConnector.fetch[TradingPremises](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.post(1)(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include(Messages("tradingpremises.want.to.register.another.premises"))

    }
  }
}
