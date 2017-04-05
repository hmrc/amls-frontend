package controllers.renewal

import connectors.DataCacheConnector
import models.businessmatching._
import models.renewal.{MsbThroughput, Renewal}
import org.mockito.ArgumentCaptor
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.RenewalService
import utils.{AuthorisedFixture, GenericTestHelper}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

class MsbThroughputControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>
    implicit val request = addToken(authRequest)

    val renewalService = mock[RenewalService]
    val dataCacheConnector = mock[DataCacheConnector]
    val renewal = Renewal()

    when {
      renewalService.getRenewal(any(), any(), any())
    } thenReturn Future.successful(Some(renewal))

    lazy val controller = new MsbThroughputController(
      self.authConnector,
      renewalService,
      dataCacheConnector
    )
  }

  trait FormSubmissionFixture extends Fixture { self =>

    val formData = "throughput" -> "01"
    val formRequest = request.withFormUrlEncodedBody(formData)
    val cache = mock[CacheMap]

    when {
      renewalService.updateRenewal(any())(any(), any(), any())
    } thenReturn Future.successful(cache)

    when {
      dataCacheConnector.fetchAll(any(), any())
    } thenReturn Future.successful(Some(cache))

    setupActivities(Set(HighValueDealing, MoneyServiceBusiness))

    def post(edit: Boolean = false)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest)))

    def setupActivities(activities: Set[BusinessActivity]) = when {
        cache.getEntry[BusinessMatching](BusinessMatching.key)
      } thenReturn Some(BusinessMatching(activities = Some(BusinessActivities(activities))))
  }

  "The MSB throughput controller" must {
    "return the view" in new Fixture {
      val result = controller.get()(request)

      status(result) mustBe OK

      contentAsString(result) must include(Messages("renewal.msb.throughput.header"))

      verify(renewalService).getRenewal(any(), any(), any())
    }

    "return a bad request result when an invalid form is posted" in new Fixture {
      val result = controller.post()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

  "A valid form post to the MSB throughput controller" must {
    "redirect to the next page in the flow if edit = false and the business is a HDV" in new FormSubmissionFixture {
      post() { result =>
        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.PercentageOfCashPaymentOver15000Controller.get().url)
      }
    }

    "redirect to the next page in the flow if edit = false and the business is not a HDV" in new FormSubmissionFixture {

      setupActivities(Set(MoneyServiceBusiness))

      post() { result =>
        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.SummaryController.get().url)
      }
    }

    "redirect to the summary page if edit = true" in new FormSubmissionFixture {
      post(edit = true) { result =>
        result.header.status mustBe SEE_OTHER
        result.header.headers.get("Location") mustBe Some(controllers.renewal.routes.SummaryController.get().url)
      }
    }

    "save the throughput model into the renewals model when posted" in new FormSubmissionFixture {
      post() { _ =>
        val captor = ArgumentCaptor.forClass(classOf[Renewal])

        verify(renewalService).updateRenewal(captor.capture())(any(), any(), any())
        captor.getValue.msbThroughput mustBe Some(MsbThroughput("01"))
      }
    }
  }
}
