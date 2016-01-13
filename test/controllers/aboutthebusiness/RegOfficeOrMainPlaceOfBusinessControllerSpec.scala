package controllers.aboutthebusiness

import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import models.aboutthebusiness.{AboutTheBusiness, BCAddress, RegOfficeOrMainPlaceOfBusiness, BusinessCustomerDetails}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.i18n.Messages
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import utils.AuthorisedFixture
import play.api.test.Helpers._


import scala.concurrent.Future

class RegOfficeOrMainPlaceOfBusinessControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new RegOfficeOrMainPlaceOfBusinessController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val businessCustomerSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]
    }
  }

  private val bCAddress = BCAddress("line_1", "line_2", Some(""), Some(""), Some("CA3 9ST"), "UK")
  private val businessCustomerDetails = BusinessCustomerDetails("businessName", Some("businessType"),
    bCAddress, "sapNumber", "safeId", Some("agentReferenceNumber"), Some("firstName"), Some("lastName"))


  "RegOfficeOrMainPlaceOfBusinessController" must {

    "Get Option:" must {

      "load register Office" in new Fixture {

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(businessCustomerDetails))
        when(controller.dataCacheConnector.fetchDataShortLivedCache[RegOfficeOrMainPlaceOfBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.confirmingyouraddress.title"))
      }

      "load Your Name page with pre populated data" in new Fixture {

        val registeredAddress = RegOfficeOrMainPlaceOfBusiness(isRegOfficeOrMainPlaceOfBusiness = true)
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(businessCustomerDetails))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[RegOfficeOrMainPlaceOfBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(registeredAddress)))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isRegOfficeOrMainPlaceOfBusiness]").`val` must be("true")
      }
    }

    "Post" must {

      "successfully redirect to the page on selection of 'Yes' this is registered address" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "true"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(businessCustomerDetails))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.BusinessRegForVATController.get().url))
      }

      "successfully redirect to the page on selection of 'No' this is not registered address" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "false"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(businessCustomerDetails))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.BusinessRegisteredWithHMRCBeforeController.get().url))
      }

      "on post invalid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "isRegOfficeOrMainPlaceOfBusiness" -> "898989"
        )
        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(businessCustomerDetails))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[name=isRegOfficeOrMainPlaceOfBusiness]").`val` must be("true")
      }
    }
  }
}
