/*
 * Copyright 2024 HM Revenue & Customs
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

package services

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
import connectors.DataCacheConnector
import models.renewal.Renewal
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ConfirmationServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  val mockCacheConnector = mock[DataCacheConnector]
  val cache = mock[CacheMap]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  val service = new ConfirmationService(mockCacheConnector)

  val credId = "123456"

  "SubmissionResponseService" when {

    "isRenewalDefined is called" must {

      "return true" when {

        "renewal is retrieved from the cache" in {

          when(mockCacheConnector.fetch[Renewal](eqTo(credId), eqTo(Renewal.key))(any(), any()))
            .thenReturn(Future.successful(Some(Renewal())))

          service.isRenewalDefined(credId).futureValue mustBe true
        }
      }

      "return false" when {

        "renewal is NOT retrieved from the cache" in {

          when(mockCacheConnector.fetch[Renewal](eqTo(credId), eqTo(Renewal.key))(any(), any()))
            .thenReturn(Future.successful(None))

          service.isRenewalDefined(credId).futureValue mustBe false
        }
      }
    }
  }
}
