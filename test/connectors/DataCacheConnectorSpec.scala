package connectors

import java.util.UUID

import builders.AuthBuilder
import config.AmlsShortLivedCache
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
  val userId = s"user-${UUID.randomUUID}"

  object TestModel{
    implicit val formats = Json.format[TestModel]
  }
  case class TestModel(name:String)

  val dummyModel:TestModel = TestModel("sample")
  val returnedCacheMap: CacheMap = CacheMap("data", Map("formId" -> Json.toJson(dummyModel)))
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
      val result = TestDataCacheConnector.saveDataShortLivedCache(sourceId, "formId", dummyModel)
      whenReady(result) { dtl =>
        dtl mustBe Some(dummyModel)
      }
    }

    "save form data to save4later with cacheId and value" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      when(mockShortLivedCache.cache(Matchers.any(), Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(returnedCacheMap))
      val result = TestDataCacheConnector.saveDataShortLivedCache("formId", dummyModel)
      whenReady(result) { dtl =>
        dtl mustBe Some(dummyModel)
      }
    }

    "fetch saved data from save4later" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockShortLivedCache.fetchAndGetEntry[TestModel](Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(dummyModel)))
      val result = TestDataCacheConnector.fetchDataShortLivedCache[TestModel](sourceId,"formId")
      whenReady(result) { dtl =>
        dtl mustBe Some(dummyModel)
      }
    }

    "fetch saved data from save4later with cacheId" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      implicit val user = AuthBuilder.createUserAuthContext(userId, "name")
      when(mockShortLivedCache.fetchAndGetEntry[TestModel](Matchers.any(),
        Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(Some(dummyModel)))
      val result = TestDataCacheConnector.fetchDataShortLivedCache[TestModel]("formId")
      whenReady(result) { dtl =>
        dtl mustBe Some(dummyModel)
      }
    }

    "fetch all data from save4later" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()
      when(mockShortLivedCache.fetch(Matchers.any())(Matchers.any())).thenReturn(Future.successful(Some(returnedCacheMap)))
      val result = TestDataCacheConnector.fetchAll(sourceId)
      whenReady(result) { dtl =>
        dtl contains("formId")
       }
    }
  }
}
