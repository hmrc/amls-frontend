package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.aboutthebusiness.{RegisteredOfficeUK, AboutTheBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future


class RegisteredOfficeControllerSpec extends PlaySpec with OneServerPerSuite with  MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegisteredOfficeController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "RegisteredOfficeController" must {

    "use correct services" in new Fixture {
      RegisteredOfficeController.authConnector must be(AMLSAuthConnector)
      RegisteredOfficeController.dataCacheConnector must be(DataCacheConnector)
    }

    val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")

    "load the where is your registered office or main place of business place page" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("aboutthebusiness.registeredoffice.title"))

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("")

    }

    "pre select uk when not in edit mode" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(None, None, None, None, Some(ukAddress), None))))

      val result = controller.get()(request)
      status(result) must be(OK)
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUK]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("")
    }

    "pre populate where is your registered office or main place of business page with saved data" in new Fixture {

      when(controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(None, None, None, None, Some(ukAddress), None))))

      val result = controller.get(true)(request)
      status(result) must be(OK)
      contentAsString(result) must include("305")
    }

    "successfully submit form and navigate to target page" in new Fixture {

      when(controller.dataCacheConnector.fetch(any())(any(), any(), any())).thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.ContactingYouController.get().url))
    }

    "fail form submission on validation error" in new Fixture {

      when(controller.dataCacheConnector.fetch(any())(any(), any(), any())).thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.save(any(), any())(any(), any(), any())).thenReturn(Future.successful(emptyCache))

      val newRequest = request.withFormUrlEncodedBody(
        "isUK"-> "true",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("There are errors in your form submission")

    }
  }
}
