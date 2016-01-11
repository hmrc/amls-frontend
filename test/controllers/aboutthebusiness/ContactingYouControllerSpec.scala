package controllers.aboutthebusiness

import java.util.UUID

import config.AMLSAuthConnector
import connectors.{BusinessCustomerSessionCacheConnector, DataCacheConnector}
import forms.AboutTheBusinessForms._
import models.ContactingYou
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

class ContactingYouControllerSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  implicit val request = FakeRequest()
  implicit val authContext = mock[AuthContext]

  val userId = s"user-${UUID.randomUUID}"
  val mockAuthConnector = mock[AuthConnector]
  val mockDataCacheConnector = mock[DataCacheConnector]
  val mockBusinessCustomerSessionCacheConnector = mock[BusinessCustomerSessionCacheConnector]

  val mockBusinessHasEmail = ContactingYou("1122334455", "test@abc.com", Some("www.google.com"), true)

  object MockContactingYouController extends ContactingYouController {
    override val authConnector = mockAuthConnector
    override val dataCacheConnector: DataCacheConnector = mockDataCacheConnector
    override val businessCustomerSessionCacheConnector = mockBusinessCustomerSessionCacheConnector
  }

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
  }

  "BusinessHasEmailController" must {
    "use correct service" in {
      ContactingYouController.authConnector must be(AMLSAuthConnector)
      ContactingYouController.dataCacheConnector must be(DataCacheConnector)
      ContactingYouController.businessCustomerSessionCacheConnector must be(BusinessCustomerSessionCacheConnector)
    }

    "on submit of page" must {
      "successfully navigate to next page " in {
        submitWithFormFilled { result =>
          status(result) must be(SEE_OTHER)
        }
      }
    }

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
  }
}
