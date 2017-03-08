package controllers.tradingpremises

import connectors.DataCacheConnector
import models.tradingpremises.{Address, RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.Matchers.{any, eq => meq}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class ActivityStartDateControllerSpec extends GenericTestHelper with ScalaFutures with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new ActivityStartDateController(messagesApi, self.authConnector, self.cache)
  }

  "ActivityStartDateController" must {

    "GET:" must {

      val pageTitle = Messages("tradingpremises.startDate.title", "firstname lastname") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      "successfully load activity start page with empty form" in new Fixture {

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get(0, false)(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe pageTitle

      }

      "successfully load activity start page with pre - populated data form" in new Fixture {

        val ytp = Some(YourTradingPremises("foo", Address("1","2",None,None,"AA1 1BB",None), None, Some(new LocalDate(2010, 10, 10)), None))

        when(controller.dataCacheConnector.fetch[Seq[TradingPremises]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(TradingPremises(Some(RegisteringAgentPremises(false)), ytp)))))

        val result = controller.get(0, false)(request)
        status(result) must be(OK)
        val document = Jsoup.parse(contentAsString(result))
        document.title mustBe pageTitle
        val dateFields = document.getElementsByClass("input--no-spinner")
        dateFields.size must be (3)
        document.getElementsByClass("form-group-day").get(0).`val` must be(10)
        document.getElementsByClass("form-group-month").get(0).`val` must be(10)
        document.getElementsByClass("form-group-year").get(0).`val` must be(2010)

      }
    }

  }
}
