package services

import connectors.DataCacheConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

class DataCacheServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object DataCacheService extends DataCacheService {
    override private[services] val cacheConnector = mock[DataCacheConnector]
  }

  val cacheMap = CacheMap("", Map.empty)

  implicit val hc = HeaderCarrier()
  implicit val ac = mock[AuthContext]

  "getCache" must {

    "Return a successful future when a cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(Some(cacheMap))

      whenReady (DataCacheService.getCache) {
        result =>
          result mustEqual cacheMap
      }
    }

    "Return a failed future when no cache exists" in {

      when {
        DataCacheService.cacheConnector.fetchAll(any(), any())
      } thenReturn Future.successful(None)

      val result = DataCacheService.getCache

      whenReady (DataCacheService.getCache.failed) {
        exception =>
          exception mustBe a[NotFoundException]
      }
    }
  }
}
