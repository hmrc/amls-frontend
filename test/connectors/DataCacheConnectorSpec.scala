package connectors

import config.AmlsShortLivedCache
import models.LoginDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.audit.http.HeaderCarrier

import scala.concurrent.Future


class DataCacheConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  val mockShortLivedCache = mock[ShortLivedCache]
  val loginDtls:LoginDetails = LoginDetails("name","password")
  val returnedCacheMap: CacheMap = CacheMap("data", Map("formId" -> Json.toJson(loginDtls)))
  val sourceId = "AMLS"

  object TestDataCacheConnector extends DataCacheConnector {
    override val shortLivedCache: ShortLivedCache = mockShortLivedCache
  }

  "DataCacheConnector" must {
    "use the correct session cache for Amls" in {
      DataCacheConnector.shortLivedCache mustBe AmlsShortLivedCache
    }

    "save form data to save4later" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockShortLivedCache.cache(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
      val result = TestDataCacheConnector.saveDataShortLivedCache(sourceId, "formId", loginDtls)
      whenReady(result) { dtl =>
        dtl mustBe Some(loginDtls)
      }
    }

    "fetch saved data from save4later" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockShortLivedCache.fetchAndGetEntry[LoginDetails](Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(loginDtls)))
      val result = TestDataCacheConnector.fetchDataShortLivedCache[LoginDetails](sourceId,"formId")
      whenReady(result) { dtl =>
        dtl mustBe Some(loginDtls)
      }
    }

    "fetch all data from save4later by utr" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockShortLivedCache.fetch(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedCacheMap)))
      val result = TestDataCacheConnector.fetchAll(sourceId)
      whenReady(result) { dtl =>
        dtl.toString must include("formId")
      }
    }
  }
}
