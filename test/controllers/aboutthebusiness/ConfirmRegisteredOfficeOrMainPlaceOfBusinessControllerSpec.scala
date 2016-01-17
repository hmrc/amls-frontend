package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import models.aboutthebusiness.{ConfirmRegisteredOfficeOrMainPlaceOfBusiness, BCAddress, BusinessCustomerDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.AuthorisedFixture
import play.api.test.Helpers._


import scala.concurrent.Future

class ConfirmRegisteredOfficeOrMainPlaceOfBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ConfirmRegisteredOfficeOrMainPlaceOfBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val businessCustomerSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]
    }
  }

  private val bCAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")
  private val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
    bCAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))


  "ConfirmRegisteredOfficeOrMainPlaceOfBusinessController" must {

    "use correct services" in new Fixture {
      ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.authConnector must be(AMLSAuthConnector)
      ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.dataCacheConnector must be(DataCacheConnector)
      ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.businessCustomerSessionCacheConnector must be(BusinessCustomerSessionCacheConnector)
    }

    "Get Option:" must {

      "load register Office" in new Fixture {

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerDetails)))
        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.confirmingyouraddress.title"))
      }

      "load Registered office or main place of business when Business Address from save4later returns None" in new Fixture {

        val registeredAddress = ConfirmRegisteredOfficeOrMainPlaceOfBusiness(isRegOfficeOrMainPlaceOfBusiness = true)

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeOrMainPlaceOfBusinessController.get().url))

      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' this is registered address" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.BusinessRegisteredForVATController.get().url))
      }

      "successfully redirect to the page on selection of 'No' this is not registered address" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "false"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.RegisteredOfficeOrMainPlaceOfBusinessController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerDetails)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        contentAsString(result) must include(Messages("This field is required"))

      }

      "on post reload the same page when businessCustomerDetails is None" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "898989"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeOrMainPlaceOfBusinessController.get().url))
      }
    }
  }
}
