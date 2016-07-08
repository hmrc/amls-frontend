package controllers.hvd

import connectors.DataCacheConnector
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import models.hvd.{PercentageOfCashPaymentOver15000, Hvd}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new PercentageOfCashPaymentOver15000Controller {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "PercentageOfCashPaymentOver15000Controller" must {

    "on get display the Percentage Of CashPayment Over 15000 page" in new Fixture {

      when(controller.dataCacheConnector.fetch[Hvd](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("hvd.percentage.title"))
    }

    "on get display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Hvd(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }


    "on post with invalid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include (Messages("error.required.hvd.percentage"))
    }

    "on post with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "percentage" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }

    "on post with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "percentage" -> "01"
      )

      when(controller.dataCacheConnector.fetch[Hvd](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[Hvd](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.hvd.routes.SummaryController.get().url))
    }
  }
}
