package controllers.renewal

import models.renewal.{MsbThroughput, Renewal}
import org.mockito.ArgumentCaptor
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import utils.{AuthorisedFixture, GenericTestHelper}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future

class MsbThroughputControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    implicit val request = addToken(authRequest)

    lazy val controller = new MsbThroughputController(
      self.authConnector,
      mock[RenewalService]
    )
  }

  trait ValidFormFixture extends Fixture {
    val formData = "throughput" -> "01"
    val formRequest = request.withFormUrlEncodedBody(formData)
  }

  "The MSB throughput controller" must {
    "return the view" in new Fixture {
      val result = controller.get()(request)

      status(result) mustBe OK

      contentAsString(result) must include(Messages("renewal.msb.throughput.header"))
    }

    "return a bad request result when an invalid form is posted" in new Fixture {
      val result = controller.post()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "A valid form post to the MSB throughput controller" must {

    lazy val fixture = new ValidFormFixture { self =>

      val renewalService = mock[RenewalService]

      override lazy val controller = new MsbThroughputController(
        self.authConnector,
        renewalService
      )

      val renewal = Renewal()

      when {
        renewalService.getRenewal(any(), any(), any())
      } thenReturn Future.successful(Some(renewal))

      when {
        renewalService.updateRenewal(any())(any(), any(), any())
      } thenReturn Future.successful(mock[CacheMap])

      lazy val result = controller.post()(formRequest)

    }

    "return a redirect result when a valid form is posted" in {
      status(fixture.result) mustBe SEE_OTHER
      redirectLocation(fixture.result) mustBe Some(controllers.renewal.routes.SummaryController.get().url)
    }

    "save the throughput model into the renewals model when posted" in {
      val captor = ArgumentCaptor.forClass(classOf[Renewal])

      verify(fixture.renewalService).updateRenewal(captor.capture())(any(), any(), any())
      captor.getValue.msbThroughput mustBe Some(MsbThroughput("01"))
    }
  }
}
