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

package connectors

import config.AmlsSessionCache
import models.status.ConfirmationStatus
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class KeystoreConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val emptyCache = CacheMap("", Map.empty)

  val amlsDataCache = mock[AmlsSessionCache]

  val connector = new KeystoreConnector(amlsDataCache)

  before {
    reset(connector.amlsDataCache)
  }

  implicit val hc = mock[HeaderCarrier]

  "confirmationIndicator" must {

    "return a successful future when the value is found" in {

      when {
        connector.amlsDataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any(), any())
      } thenReturn Future.successful(Some(ConfirmationStatus(Some(true))))

      whenReady(connector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(Some(true))
      }

    }

    "return an empty successful future when the value is not found" in {
      when {
        connector.amlsDataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any(), any())
      } thenReturn Future.successful(None)

      whenReady(connector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(None)
      }

    }

    "save the confirmation status into the keystore" in {

      when {
        connector.amlsDataCache.cache(any(), any())(any(), any(), any())
      } thenReturn Future.successful(emptyCache)

      whenReady(connector.setConfirmationStatus) { _ =>
        verify(connector.amlsDataCache).cache(eqTo(ConfirmationStatus.key), eqTo(ConfirmationStatus(Some(true))))(any(), any(), any())
      }

    }

    "remove the confirmation status from the keystore" in {

      when {
        connector.amlsDataCache.cache(any(), any())(any(), any(), any())
      } thenReturn Future.successful(emptyCache)

      whenReady(connector.resetConfirmation) { _ =>
        verify(connector.amlsDataCache).cache(eqTo(ConfirmationStatus.key), eqTo(ConfirmationStatus(None)))(any(), any(), any())
      }

    }

  }
}
