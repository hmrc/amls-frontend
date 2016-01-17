package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import forms.Form2
import models.aboutthebusiness.{RegisteredOfficeOrMainPlaceOfBusiness, RegisteredOfficeOrMainPlaceOfBusinessUK, AboutTheBusiness}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import utils.AuthorisedFixture
import play.api.test.Helpers._

import scala.concurrent.Future


class RegisteredOfficeOrMainPlaceOfBusinessControllerSpec extends PlaySpec with OneServerPerSuite with  MockitoSugar{

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegisteredOfficeOrMainPlaceOfBusinessController () {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "RegisteredOfficeOrMainPlaceOfBusinessController" must {

    "use correct services" in new Fixture {
      RegisteredOfficeOrMainPlaceOfBusinessController.authConnector must be(AMLSAuthConnector)
      RegisteredOfficeOrMainPlaceOfBusinessController.dataCacheConnector must be(DataCacheConnector)
    }

    val ukAddress = RegisteredOfficeOrMainPlaceOfBusinessUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")

    "load the where is your registered office or main place of business place page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("aboutthebusiness.registeredoffice.title"))
      val document = Jsoup.parse(contentAsString(result))
      document.select("input[name=isUKOrOverseas]").`val` must be("true")
      document.select("input[name=addressLine2]").`val` must be("Longbenton")
    }
    "some" in {
      val model = RegisteredOfficeOrMainPlaceOfBusinessUK("38Bxxxx", "Longbenton", None, None, "NE7 7DX")
      val test = Form2[RegisteredOfficeOrMainPlaceOfBusiness](model)
      println("-----------------------------"+test)
    }
    "load the where is your registered office or main place of business place page with uk radio option as selected" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(None))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include (Messages("aboutthebusiness.registeredoffice.title"))
    }


    "pre populate where is your registered office or main place of business page with saved data" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())(any(), any(), any())).
        thenReturn(Future.successful(Some(AboutTheBusiness(None, None, Some(ukAddress)))))
      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include("305")
    }

    "submit form and navigate to target page" in new Fixture {
      when(controller.dataCacheConnector.fetchDataShortLivedCache(any())(any(), any(), any())).thenReturn(Future.successful(None))
      when (controller.dataCacheConnector.saveDataShortLivedCache(any(), any())(any(), any(), any())).thenReturn(Future.successful(None))
      val newRequest = request.withFormUrlEncodedBody(
        "isUKOrOverseas"-> "true",
        "addressLine1"->"line1",
        "addressLine2"->"line2",
        "addressLine3"->"",
        "addressLine4"->"",
        "postCode"->"NE7 7DS")
      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(routes.BusinessRegisteredForVATController.get().url))
    }
  }
}
