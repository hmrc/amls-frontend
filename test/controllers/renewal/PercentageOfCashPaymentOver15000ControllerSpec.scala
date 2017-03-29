package controllers.renewal

import connectors.DataCacheConnector
import models.renewal.{PercentageOfCashPaymentOver15000, Renewal}
import models.status.{NotCompleted, SubmissionDecisionApproved}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, GenericTestHelper}

import scala.concurrent.Future

class PercentageOfCashPaymentOver15000ControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures{

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new PercentageOfCashPaymentOver15000Controller(
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService,
      statusService = mockStatusService
    )
  }

  val emptyCache = CacheMap("", Map.empty)

  "PercentageOfCashPaymentOver15000Controller" must {

    "on get display the Percentage Of CashPayment Over 15000 page" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Renewal](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("hvd.percentage.title"))
    }

    "on get display the Percentage Of CashPayment Over 15000 page with pre populated data" in new Fixture {

      when(controller.statusService.getStatus(any(), any(), any()))
        .thenReturn(Future.successful(NotCompleted))

      when(controller.dataCacheConnector.fetch[Renewal](any())
        (any(), any(), any())).thenReturn(Future.successful(Some(Renewal(percentageOfCashPaymentOver15000 = Some(PercentageOfCashPaymentOver15000.First)))))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document = Jsoup.parse(contentAsString(result))
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

  }
}
