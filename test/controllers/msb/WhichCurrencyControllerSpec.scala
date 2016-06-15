package controllers.msb

import connectors.DataCacheConnector
import models.moneyservicebusiness.{WhichCurrencies, MoneyServiceBusiness}
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture
import play.api.test.Helpers._
import play.api.http.Status.{SEE_OTHER, BAD_REQUEST}
import play.api.http.HeaderNames.LOCATION
import views.html.msb.which_currencies

import scala.concurrent.Future

class WhichCurrencyControllerSpec extends WordSpec
                                    with MockitoSugar
                                    with MustMatchers
                                    with OneAppPerSuite
                                    with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val cache: DataCacheConnector = mock[DataCacheConnector]

    val controller = new WhichCurrenciesController {
      override def cache: DataCacheConnector = self.cache
      override protected def authConnector: AuthConnector = self.authConnector
    }
  }

  "WhichCurrencyController" when {
    "get is called" when {
      "succeed" in new Fixture {
        when(cache.fetch[MoneyServiceBusiness](eqTo(MoneyServiceBusiness.key))(any(), any(), any()))
          .thenReturn(Future.successful(None))

        val resp = controller.get(false).apply(request)
        status(resp) must be(200)
      }
    }

    "post is called " when {
      "data is valid" should {
        "redirect to check your answers" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody (
            "currencies[0]" -> "USD",
            "currencies[1]" -> "GBP",
            "currencies[2]" -> "EUR",
            "bankMoneySource" ->"Yes",
            "bankNames" ->"Bank names",
            "wholesalerMoneySource" -> "Yes",
            "wholesalerNames" -> "wholesaler names",
            "customerMoneySource" -> "Yes"
          )

          val result = controller.post(false).apply(request)

          println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
          println(contentAsString(result))
          println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
          status(result) must be (SEE_OTHER)
          header(LOCATION, result) must be ("lfsgkfjhdfkjhjk")
        }
      }

      "data is invalid" should {
        "return invalid form" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankMoneySource" ->"Yes",
            "bankNames" ->"Bank names",
            "wholesalerMoneySource" -> "Yes",
            "wholesalerNames" -> "wholesaler names",
            "customerMoneySource" -> "Yes"
          )
          whenReady(controller.post(false).apply(request)) {
            resp =>
              resp.header.status must be(BAD_REQUEST)
          }
        }
      }
    }
  }
}
