package controllers.aboutthebusiness

import connectors.DataCacheConnector
import models.aboutthebusiness.{CorporationTaxRegisteredYes, AboutTheBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class CorporationTaxRegisteredControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new CorporationTaxRegisteredController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "CorporationTaxRegisteredController" must {

    "on get display the registered for corporation tax page" in new Fixture {
      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("aboutthebusiness.registeredforcorporationtax.title"))
    }

    "on get display the registered for corporation tax page with pre populated data" in new Fixture {

      val data = AboutTheBusiness(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1234567890")))

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(data)))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
      document.getElementById("corporationTaxReference").`val` must be("1234567890")
    }

    "on post with valid data and edit false continue to registered office page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> "1234567890"
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
    }

    "on post with valid data and edit true redirect to summary page" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "true",
        "corporationTaxReference" -> "1234567890"
      )

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
        (any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
    }

    "on post with invalid data show an error message" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "registeredForCorporationTax" -> "invalid"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error-summary-heading").text must be(Messages("err.summary"))
    }

  }

}
