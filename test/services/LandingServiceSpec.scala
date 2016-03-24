package services

import connectors.{DataCacheConnector, KeystoreConnector}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class LandingServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object LandingService extends LandingService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
    override private[services] val keyStore = mock[KeystoreConnector]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]

  "hasSavedFrom" must {

    "return true if a cache exists" in {
      when {
        LandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(CacheMap("", Map.empty)))
      whenReady (LandingService.hasSavedForm) {
        _ mustEqual true
      }
    }

    "return false if a cache does not exist" in {
      when {
        LandingService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)
      whenReady (LandingService.hasSavedForm) {
        _ mustEqual false
      }
    }
  }

  "reviewDetails" must {

    "pass through from the keystore connector" in {
      when {
        LandingService.keyStore.optionalReviewDetails(any(), any())
      }
    }
  }
}
