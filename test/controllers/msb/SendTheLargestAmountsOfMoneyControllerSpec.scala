package controllers.msb

import connectors.DataCacheConnector
import models.Country
import models.moneyservicebusiness.{SendTheLargestAmountsOfMoney, MoneyServiceBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyControllerSpec extends PlaySpec with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SendTheLargestAmountsOfMoneyController {

      override val dataCacheConnector: DataCacheConnector = mock[DataCacheConnector]
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendTheLargestAmountsOfMoneyController" must {

    "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.title() must be (Messages("msb.send.the.largest.amounts.of.money.title"))
    }

    "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture  {

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(MoneyServiceBusiness(
          sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB")))))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("select[name=country_1] > option[value=GB]").hasAttr("selected") must be(true)

    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GS"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> "GS"
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.SummaryController.get().url))
    }

    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "country_1" -> ""
      )

      when(controller.dataCacheConnector.fetch[MoneyServiceBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[MoneyServiceBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)

      val document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#country_1]").html() must include(Messages("error.required.country.name"))
    }
  }
}
