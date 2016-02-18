package controllers.aboutthebusiness

import java.util.UUID

import connectors.DataCacheConnector
import models.aboutthebusiness.{AboutTheBusiness, ContactingYou, RegisteredOfficeUK}
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
  val contactingYou = Some(ContactingYou("1234567890", "test@test.com"))
  val ukAddress = RegisteredOfficeUK("305", "address line", Some("address line2"), Some("address line3"), "NE7 7DX")
  val aboutTheBusinessWithData = AboutTheBusiness(contactingYou = contactingYou, registeredOffice = Some(ukAddress))

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new ContactingYouController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
    }
  }

  "BusinessHasEmailController" must {

    "Get" must {

      "load the page" in new Fixture {

        val aboutTheBusinessWithAddress = AboutTheBusiness(registeredOffice = Some(ukAddress))

        when(controller.dataCache.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithAddress)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }

      "load the page with the pre populated data" in new Fixture {

        when(controller.dataCache.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include(Messages("aboutthebusiness.contactingyou.title"))
      }
    }

    "Post" must {

      "on post of valid data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "1234567890",
          "email" -> "test@test.com",
          "website" -> "website",
          "letterToThisAddress" -> "true"
        )

        when(controller.dataCache.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.post()(newRequest)
        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(routes.SummaryController.get().url))
      }


      "on post of incomplete data" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody(
          "phoneNumber" -> "1234567890"
        )

        when(controller.dataCache.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCache.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }

}
