package controllers.hvd

import connectors.DataCacheConnector
import models.hvd.Hvd
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import utils.AuthorisedFixture
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class ReceiveCashPaymentsControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ReceiveCashPaymentsController {
      override val cacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }

    when(controller.cacheConnector.fetch[Hvd](eqTo(Hvd.key))(any(), any(), any()))
      .thenReturn(Future.successful(None))

    when(controller.cacheConnector.save[Hvd](eqTo(Hvd.key), any())(any(), any(), any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "ReceiveCashPaymentsController" must {

    "load the page" in new Fixture {

      val result = controller.get()(request)
      status(result) mustEqual OK
    }

    "show a bad request with an invalid request" in new Fixture {

      val result = controller.post()(request)
      status(result) mustEqual BAD_REQUEST
    }

    "redirect to summary on successful edit" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(true)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.SummaryController.get().url)
    }

    "redirect to next page on successful submission" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "receivePayments" -> "false"
      )

      val result = controller.post(false)(newRequest)

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustEqual Some(routes.PercentageOfCashPaymentOver15000Controller.get().url)
    }
  }
}
