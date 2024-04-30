/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.DataCacheConnector
import helpers.IntegrationBaseSpec
import models.businessmatching.BusinessActivity.BillPaymentServices
import models.businessmatching.{BusinessActivities, BusinessMatching}
import play.api.libs.json.Json
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import services.encryption.EncryptionService
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class DataCacheConnectorISpec extends IntegrationBaseSpec with DefaultPlayMongoRepositorySupport[Cache] {

  val cacheClientFactory =
    new MongoCacheClientFactory(appConfig, app.injector.instanceOf[ApplicationCrypto], mongoComponent, app.injector.instanceOf[EncryptionService])
  override val repository: MongoCacheClient = cacheClientFactory.createClient
  val cacheConnector = new MongoCacheConnector(cacheClientFactory)
  val dataCacheConnector: DataCacheConnector = new DataCacheConnector(cacheConnector)

  ".fetch" must {

    "retrieve an item from the cache given a model type" in {
      val businessMatching = BusinessMatching(activities = Some(BusinessActivities(Set(BillPaymentServices))))
      val testCache: Cache = Cache("123", Map(BusinessMatching.key -> Json.toJson(businessMatching)))
      repository.saveAll(testCache, "123")
      dataCacheConnector.fetch[BusinessMatching]("123", BusinessMatching.key).futureValue mustBe Some(businessMatching)
    }
  }
}
