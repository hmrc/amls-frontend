package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.aboutthebusiness._
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import play.api.test.Helpers._


import scala.concurrent.Future

class ConfirmRegisteredOfficeControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ConfirmRegisteredOfficeController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  private val ukAddress = RegisteredOfficeUK("line_1", "line_2", Some(""), Some(""), "CA3 9ST")
  private val aboutTheBusiness = AboutTheBusiness(None, None, None, None, Some(ukAddress), None)

  "ConfirmRegisteredOfficeController" must {

    "use correct services" in new Fixture {
      ConfirmRegisteredOfficeController.authConnector must be(AMLSAuthConnector)
      ConfirmRegisteredOfficeController.dataCache must be(DataCacheConnector)
    }

    "Get Option:" must {

      "load register Office" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.confirmingyouraddress.title"))
      }

      "load Registered office or main place of business when Business Address from save4later returns None" in new Fixture {

        val registeredAddress = ConfirmRegisteredOffice(isRegOfficeOrMainPlaceOfBusiness = true)

        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeController.get().url))

      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' [this is registered address]" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ContactingYouController.get().url))
      }

      "successfully redirect to the page on selection of Option 'No' [this is not registered address]" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "false"
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("This field is required"))

      }

      "on post with invalid data show error" in new Fixture {
        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> ""
        )
        when(controller.dataCache.fetch[AboutTheBusiness](any())(any(),any(),any()))
          .thenReturn(Future.successful(Some(aboutTheBusiness)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include ("There are errors in your form submission")

      }
    }
  }
}
