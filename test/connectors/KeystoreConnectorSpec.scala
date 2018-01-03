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

package connectors

import models.status.ConfirmationStatus
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class KeystoreConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures with BeforeAndAfter {

  val emptyCache = CacheMap("", Map.empty)

  object KeystoreConnector extends KeystoreConnector {
    override private[connectors] val amlsDataCache: SessionCache = mock[SessionCache]
  }

  before {
    reset(KeystoreConnector.amlsDataCache)
  }

  implicit val hc = mock[HeaderCarrier]

  "confirmationIndicator" must {

    "return a successful future when the value is found" in {

      when {
        KeystoreConnector.amlsDataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any(), any())
      } thenReturn Future.successful(Some(ConfirmationStatus(Some(true))))

      whenReady(KeystoreConnector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(Some(true))
      }

    }

    "return an empty successful future when the value is not found" in {
      when {
        KeystoreConnector.amlsDataCache.fetchAndGetEntry[ConfirmationStatus](eqTo(ConfirmationStatus.key))(any(), any(), any())
      } thenReturn Future.successful(None)

      whenReady(KeystoreConnector.confirmationStatus) { result =>
        result mustBe ConfirmationStatus(None)
      }

    }

    "save the confirmation status into the keystore" in {

      when {
        KeystoreConnector.amlsDataCache.cache(any(), any())(any(), any(), any())
      } thenReturn Future.successful(emptyCache)

      whenReady(KeystoreConnector.setConfirmationStatus) { _ =>
        verify(KeystoreConnector.amlsDataCache).cache(eqTo(ConfirmationStatus.key), eqTo(ConfirmationStatus(Some(true))))(any(), any(), any())
      }

    }

    "remove the confirmation status from the keystore" in {

      when {
        KeystoreConnector.amlsDataCache.cache(any(), any())(any(), any(), any())
      } thenReturn Future.successful(emptyCache)

      whenReady(KeystoreConnector.resetConfirmation) { _ =>
        verify(KeystoreConnector.amlsDataCache).cache(eqTo(ConfirmationStatus.key), eqTo(ConfirmationStatus(None)))(any(), any(), any())
      }

    }

  }
}
