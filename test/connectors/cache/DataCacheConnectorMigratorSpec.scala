/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors.cache

import connectors.cache.DataCacheConnectorMigrator
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}

import scala.concurrent.Future

class DataCacheConnectorMigratorSpec
  extends PlaySpec
    with OneAppPerSuite
    with MockitoSugar
    with ScalaFutures
    with IntegrationPatience {

  case class Model(value: String)
  object Model {
    implicit val format = Json.format[Model]
  }

  trait Fixture {

    implicit val headerCarrier = HeaderCarrier()
    implicit val authContext = mock[AuthContext]
    implicit val user = mock[LoggedInUser]
    val oid = "user_oid"
    val key = "key"
    val cacheId = "12345"

    implicit val newDataCacheConnectorMock = mock[DataCacheConnector]
    implicit val currentDataCacheConnectorMock = mock[DataCacheConnector]

    val migrator:DataCacheConnectorMigrator = new DataCacheConnectorMigrator(newDataCacheConnectorMock, currentDataCacheConnectorMock)

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn oid
  }

  val emptyCache = CacheMap("", Map.empty)

  "DataCacheConnectorMigrator" must {

    "fetch data from the new connector, if the data exists the new connector" in new Fixture {
      val model = Model("data")
      when {
        newDataCacheConnectorMock.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful[Option[Model]](Some(model))

      val result = migrator.fetch[Model](cacheId)
      whenReady(result) {
        result => result must be(Some(model))
      }
    }

    "fetch data from the current data connector, if the data doesn't doesn't exist in the new data connector, but does in the current data connector - the data should be written into the new dataconnector" in new Fixture {
      val model = Model("data")
      when {
        newDataCacheConnectorMock.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful(None)

      when {
        currentDataCacheConnectorMock.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful[Option[Model]](Some(model))

      val result = migrator.fetch[Model](cacheId)
      whenReady(result) {
        result => result must be(Some(model))
      }
      verify(newDataCacheConnectorMock).save[Model](any(),any())(any(), any(), any())
    }

    "return a record not found when fetching data, if there is no data in either" in new Fixture {
      when {
        newDataCacheConnectorMock.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful(None)

      when {
        currentDataCacheConnectorMock.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful(None)

      val result = migrator.fetch[Model](cacheId)
      whenReady(result) {
        result => result must be(None)
      }
    }

    "fetch all data from the new connector, if the data exists in the new connector" in new Fixture {
      when {
        newDataCacheConnectorMock.fetchAll
      } thenReturn Future.successful[Option[CacheMap]](Some(emptyCache))

      val result = migrator.fetchAll
      whenReady(result) {
        result => result must be(Some(emptyCache))
      }
      // verify(newDataCacheConnectorMock).save[CacheMap](any(),any())(any(), any(), any())
    }

    "fetch all data from the current data connector, if the data doesn't exist in the new data connector, but does in the current data connector - the data should be written into the new dataconnector" in new Fixture {
      when {
        newDataCacheConnectorMock.fetchAll
      } thenReturn Future.successful(None)
      when {
        currentDataCacheConnectorMock.fetchAll
      } thenReturn Future.successful[Option[CacheMap]](Some(emptyCache))

      val result = migrator.fetchAll
      whenReady(result) {
        result => result must be(Some(emptyCache))
      }
    }

    "return a record not found when fetching all data, if there is no data in either" in new Fixture {
      when {
        newDataCacheConnectorMock.fetchAll
      } thenReturn Future.successful(None)
      when {
        currentDataCacheConnectorMock.fetchAll
      } thenReturn Future.successful(None)

      val result = migrator.fetchAll
      whenReady(result) {
        result => result must be(None)
      }
    }

    "Save data should save the data using the new connector" in new Fixture {
      val model = Model("data")
      when {
        newDataCacheConnectorMock.save[Model](any(), any())(any(), any(), any())
      } thenReturn Future.successful[CacheMap](emptyCache)

      val result = migrator.save[Model](key, model)
      whenReady(result) {
        result => result must be(emptyCache)
      }
    }

    "Remove data should remove the data from the new connector" in new Fixture {
      val response = mock[HttpResponse]
      when {
        newDataCacheConnectorMock.remove(any(), any())
      } thenReturn Future.successful[HttpResponse](response)


      val result = migrator.remove
      whenReady(result) {
        result => result must be(response)
      }
    }

    "Update should update the data in the new connector" in new Fixture {
      val model = Model("data")
      when {
        newDataCacheConnectorMock.update[Model](any())(any())(any(), any(), any())
      } thenReturn Future.successful[Option[Model]](Some(model))

      val result = migrator.update[Model](cacheId)(f => model)
      whenReady(result) {
        result => result must be(Some(model))
      }
    }
  }
}
