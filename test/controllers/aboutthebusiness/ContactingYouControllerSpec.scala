package controllers.aboutthebusiness

import java.util.UUID

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, ContactingYou, RegisteredOfficeUK}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactingYouControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val contactingYou = Some(ContactingYou("+44 (0)123 456-7890", "test@test.com"))
  val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")
  val aboutTheBusinessWithData = AboutTheBusiness(contactingYou = contactingYou, registeredOffice = Some(ukAddress))

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ContactingYouController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  val emptyCache = CacheMap("", Map.empty)

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page" in new Fixture {

        val aboutTheBusinessWithAddress = AboutTheBusiness(registeredOffice = Some(ukAddress))

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithAddress)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with no data" in new Fixture {
        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))
        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.ConfirmRegisteredOfficeController.get().url)
      }

    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "true"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }


      "on post of incomplete data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }


      "on post of incomplete data with no response from data cache" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.ContactingYouController.get().url)
      }

      "load the page with valid data and letterToThisAddress set to false" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "+44 (0)123 456-7890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "false"
        )

        when(controller.dataCache.fetch[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.save[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) mustBe Some(routes.CorrespondenceAddressController.get().url)
      }

    }
  }

}