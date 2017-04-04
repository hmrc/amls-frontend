package controllers.renewal

import cats.implicits._
import models.renewal.{MsbMoneyTransfers, Renewal}
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class MsbMoneyTransferControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    val renewalService = mock[RenewalService]
    val request = addToken(authRequest)

    lazy val controller = new MsbMoneyTransfersController(self.authConnector, renewalService)
  }

  trait FormSubmissionFixture extends Fixture {

    when {
      renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Some(Renewal()))

  }

  "Calling the GET action" must {
    "return the correct view" when {
      "edit is false" in new Fixture {
        val result = controller.get()(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select(".heading-xlarge").text mustBe Messages("renewal.msb.transfers.header")
      }

      "edit is true" in new Fixture {
        val result = controller.get(true)(request)

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("form").first.attr("action") mustBe routes.MsbMoneyTransfersController.post(true).url
      }
    }
  }

  "Calling the POST action" when {
    "posting valid data" must {
      "redirect to the next page in the flow" ignore new FormSubmissionFixture {
        val result = controller.post()(request)

        status(result) mustBe SEE_OTHER
      }

      "save the model data into the renewal object" ignore new FormSubmissionFixture {
        val form = "transfers" -> "1500"
        val result = controller.post()(request.withFormUrlEncodedBody(form))

        val captor = ArgumentCaptor.forClass(classOf[Renewal])
        verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())

        captor.getValue.msbTransfers mustBe Some(MsbMoneyTransfers(1500))
      }
    }
  }
}
