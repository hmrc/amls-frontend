package controllers.aboutthebusiness

import java.util.UUID

import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import models.{AboutTheBusiness, ContactingYou}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.test.Helpers._
import utils.AuthorisedFixture

import scala.concurrent.Future

class ContactingYouControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val userId = s"user-${UUID.randomUUID}"
  val contactingYou = Some(ContactingYou("1234567890", "test@test.com", "http://mywebsite.co.uk"))

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

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)
        //val jsonObject = JsObject(Seq("name" -> JsString("name Value")))
        //contentAsString(result) must include(Messages("aboutthebusiness.sectiontitle"))
      }

      "load the page with the pre populated data" in new Fixture {

        val aboutTheBusinessWithData = AboutTheBusiness(contactingYou)

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.get()(request)
        status(result) must be(OK)
        /*
          val document = Jsoup.parse(contentAsString(result))
          document.select("input[name=phoneNumber]").`val` must be("1234567890")
          document.select("input[name=email]").`val` must be("test@test.com")
          document.select("input[name=website]").`val` must be("test@test.com")

          contentAsString(result) must include(aboutTheBusinessWithData.contactingYou.fold("") {_.phoneNumber})
          contentAsString(result) must include(aboutTheBusinessWithData.contactingYou.fold("") {_.email})
          contentAsString(result) must include(aboutTheBusinessWithData.contactingYou.fold("") {_.website})
        */
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

        val aboutTheBusinessWithData = AboutTheBusiness(contactingYou)

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

        val aboutTheBusinessWithData = AboutTheBusiness(contactingYou)

        when(controller.dataCacheConnector.fetchDataShortLivedCache[AboutTheBusiness](any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        when(controller.dataCacheConnector.saveDataShortLivedCache[AboutTheBusiness](any(), any())
          (any(), any(), any())).thenReturn(Future.successful(Some(aboutTheBusinessWithData)))

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
        //redirectLocation(result) must be(Some(routes.ContactingYouController.get().url))
        /*
                val document = Jsoup.parse(contentAsString(result))
                document.select("input[name=phoneNumber]").`val` must be("1234567890")
        */
      }


    }
  }
  /*
      def createBusinessHasEmailFormForSubmission(test: Future[Result] => Any, email: String) {
        val mockBusinessHasEmail = ContactingYou(email)
        val form  = contactingYouForm.fill(mockBusinessHasEmail)
        val fakePostRequest = FakeRequest("POST", "/business-has-Email").withFormUrlEncodedBody(form.data.toSeq: _*)
        when(mockDataCacheConnector.saveDataShortLivedCache[ContactingYou](Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(mockBusinessHasEmail)))
        val result = MockContactingYouController.post(mock[AuthContext], fakePostRequest)
        test(result)
      }

      def submitWithFormFilled(test: Future[Result] => Any) {
        createBusinessHasEmailFormForSubmission(test, "test@google.com")
      }
  */

}
