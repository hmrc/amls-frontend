package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.bankdetails._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "Get" must {

    "use correct services" in new Fixture {
      SummaryController.authConnector must be(AMLSAuthConnector)
      SummaryController.dataCache must be(DataCacheConnector)
    }

    "load the summary page when section data is available" in new Fixture {

      val model = BankDetails(None, None)

      when(controller.dataCache.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Seq(model))))
      val result = controller.get()(request)

      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {
      when(controller.dataCache.fetch[Seq[BankDetails]](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }
  }
}
