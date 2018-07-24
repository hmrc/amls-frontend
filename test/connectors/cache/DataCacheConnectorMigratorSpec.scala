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

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.libs.json.Json
import services.cache.Cache
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser}
import utils.AmlsSpec
import org.scalacheck.Arbitrary.arbitrary

import scala.concurrent.Future

class DataCacheConnectorMigratorSpec extends AmlsSpec
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

    implicit val primaryConnector = mock[MongoCacheConnector]
    implicit val fallbackConnector = mock[Save4LaterCacheConnector]

    val migrator = new DataCacheConnectorMigrator(primaryConnector, fallbackConnector)

    when(authContext.user) thenReturn user
    when(user.oid) thenReturn oid
  }

  val emptyCache = CacheMap("", Map.empty)

  "DataCacheConnectorMigrator" must {

    "fetch data from the new connector, if the data exists the new connector" in new Fixture {
      val model = Model("data")

      when {
        primaryConnector.fetch[Model](eqTo(cacheId))(any(), any(), any())
      } thenReturn Future.successful[Option[Model]](Some(model))

      val result = migrator.fetch[Model](cacheId)

      whenReady(result) {
        result => result mustBe Some(model)
      }
    }

    "write data to the primary cache" when {

      "saving a single model" in new Fixture {
        val model = Model("data")

        when {
          primaryConnector.save[Model](any(), any())(any(), any(), any())
        } thenReturn Future.successful[CacheMap](emptyCache)

        val result = migrator.save[Model](key, model)

        whenReady(result) { result =>
          result mustBe emptyCache
          verify(primaryConnector).save[Model](eqTo(key), eqTo(model))(any(), any(), any())
        }
      }

      "migrating the entire cache" in new Fixture {
        when {
          primaryConnector.fetchAll
        } thenReturn Future.successful(None)

        when {
          fallbackConnector.fetchAll
        } thenReturn Future.successful[Option[CacheMap]](Some(emptyCache))

        when {
          primaryConnector.saveAll(emptyCache)
        } thenReturn Future.successful(Cache(emptyCache))

        val result = migrator.fetchAll

        whenReady(result) { result =>
          result mustBe Some(emptyCache)
        }
      }

      "data has been loaded from the fallback cache and needs to be migrated" in new Fixture {
        val model = Model("data")
        override val key = arbitrary[String].sample.get
        val cache = CacheMap("", Map(key -> Json.toJson(model)))

        when {
          primaryConnector.fetch[Model](key)
        } thenReturn Future.successful(None)

        when {
          fallbackConnector.fetchAll
        } thenReturn Future.successful(Some(cache))

        when {
          primaryConnector.saveAll(cache)
        } thenReturn Future.successful(Cache("", Map.empty))

        val result = migrator.fetch[Model](key)

        whenReady(result) { result =>
          result mustBe Some(model)
          verify(primaryConnector).saveAll(cache)
        }
      }
    }

    "return None if there is no data in either cache" in new Fixture {
      when {
        primaryConnector.fetch[Model](cacheId)
      } thenReturn Future.successful(None)

      when {
        fallbackConnector.fetchAll
      } thenReturn Future.successful(None)

      val result = migrator.fetch[Model](cacheId)

      whenReady(result) { _ mustBe None }
    }

    "fetch all data from the primary connector, if the data exists in the primary connector" in new Fixture {
      when {
        primaryConnector.fetchAll
      } thenReturn Future.successful[Option[CacheMap]](Some(emptyCache))

      val result = migrator.fetchAll

      whenReady(result) { _ mustBe Some(emptyCache) }
    }

    "return a record not found when fetching all data, if there is no data in either" in new Fixture {
      when {
        primaryConnector.fetchAll
      } thenReturn Future.successful(None)

      when {
        fallbackConnector.fetchAll
      } thenReturn Future.successful(None)

      val result = migrator.fetchAll
      whenReady(result) { _ mustBe None }
    }


    "remove the data from the new connector" in new Fixture {
      when {
        primaryConnector.remove(any(), any())
      } thenReturn Future.successful(true)

      when {
        fallbackConnector.remove(any(), any())
      } thenReturn Future.successful(true)

      val result = migrator.remove

      whenReady(result) { _ mustBe true }

      verify(primaryConnector).remove(any(), any())
      verify(fallbackConnector).remove(any(), any())
    }

    "update the data in the new connector" in new Fixture {
      val model = Model("data")

      when {
        primaryConnector.update[Model](any())(any())(any(), any(), any())
      } thenReturn Future.successful[Option[Model]](Some(model))

      val result = migrator.update[Model](cacheId)(_ => model)

      whenReady(result) { _ mustBe Some(model) }
    }
  }
}
