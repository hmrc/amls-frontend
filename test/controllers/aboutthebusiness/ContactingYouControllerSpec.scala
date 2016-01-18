package controllers.aboutthebusiness

import java.util.UUID

import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import models.aboutthebusiness.{AboutTheBusiness, BCAddress, BusinessCustomerDetails, ContactingYou}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactingYouControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val contactingYou = Some(ContactingYou("1234567890", "test@test.com", "http://mywebsite.co.uk"))
  val aboutTheBusinessWithData = AboutTheBusiness(None, None, contactingYou)
  val businessAddress = BCAddress("line_1", "2", Some("3"), Some("4"), Some("5"), "UK")
  val businessCustomerData = BusinessCustomerDetails("name", Some("business_type"), businessAddress, "12345", "2345678", Some("12345678"), Some("John"), Some("San"))

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ContactingYouController {
      override val dataCacheConnector = mock[DataCacheConnector]
      override val businessCustomerSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page" in new Fixture {

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerData)))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        //val jsonObject = JsObject(Seq("name" -> JsString("name Value")))
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerData)))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
      }
    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "1234567890",
          "email" -> "test@test.com",
          "website" -> "http://mywebsite.co.uk",
          "sendLettersToThisAddress" -> "true"
        )

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerData)))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.ContactingYouController.get().url))
      }


      "on post of incomplete data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "1234567890"
        )

        when(controller.businessCustomerSessionCacheConnector.getReviewBusinessDetails[BusinessCustomerDetails](any(), any()))
          .thenReturn(Future.successful(Some(businessCustomerData)))

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }

}
