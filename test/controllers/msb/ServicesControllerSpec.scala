package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness.{CurrencyExchange, MoneyServiceBusiness, MsbServices, TransmittingMoney}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import org.scalatest.mock.MockitoSugar
import utils.AuthorisedFixture
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import play.api.test.Helpers._
import org.jsoup.Jsoup

import scala.concurrent.Future

class ServicesControllerSpec extends PlaySpec with ScalaFutures with MockitoSugar with OneServerPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new ServicesController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "ServicesController" must {

    "show an empty form on get with no data in store" in new Fixture {

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[name=msbServices[]]").size mustBe 4
      document.select("input[name=msbServices[]][checked]").size mustBe 0
    }

    "show a prefilled form when there is data in the store" in new Fixture {

      val model = MoneyServiceBusiness(
        msbServices = Some(
          MsbServices(Set(TransmittingMoney, CurrencyExchange))
        )
      )

      when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
        .thenReturn(Future.successful(Some(model)))

      val result = controller.get()(request)
      val document = Jsoup.parse(contentAsString(result))

      status(result) mustBe OK

      document.select("input[name=msbServices[]][checked]").size mustBe 2
      document.select("input[value=01]").hasAttr("checked") mustBe true
      document.select("input[value=02]").hasAttr("checked") mustBe true
    }
  }
}
