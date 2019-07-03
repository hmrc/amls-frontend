package controllers.renewal

import connectors.DataCacheConnector
import models.renewal.{PaymentMethods, ReceiveCashPayments}
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import services.RenewalService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture}

import scala.concurrent.Future

class CashPaymentsCustomersNotMetControllerSpec extends AmlsSpec {

  lazy val mockDataCacheConnector = mock[DataCacheConnector]
  lazy val mockRenewalService = mock[RenewalService]

  val receiveCashPayments = ReceiveCashPayments(
    Some(PaymentMethods(true, true,Some("other"))
    ))

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new ReceiveCashPaymentsController (
      dataCacheConnector = mockDataCacheConnector,
      authConnector = self.authConnector,
      renewalService = mockRenewalService
    )

    when(mockRenewalService.getRenewal(any(),any(),any()))
      .thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any())(any(),any(),any()))
      .thenReturn(Future.successful(new CacheMap("", Map.empty)))
  }

  "CashPaymentsCustomersNotMet" when {
    "get is called" must {
      "load the page" when {
        "renewal data is found for receiving payments and pre-populate the data" in new Fixture {

        }
      }
    }

    "post is called" must {

    }
  }
}
