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

package services.businessdetails

import connectors.DataCacheConnector
import models.businessdetails.{BusinessDetails, ContactingYou, ContactingYouEmail}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class BusinessEmailAddressServiceSpec extends AmlsSpec with BeforeAndAfterEach with IntegrationPatience {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap = mock[Cache]

  val service = new BusinessEmailAddressService(mockCacheConnector)

  val credId = "123456"
  val email = "person@domain.com"

  override def beforeEach(): Unit = reset(mockCacheConnector, mockCacheMap)

  "BusinessEmailAddressService" when {

    "getEmailAddress is called" must {

      "return the email address when it is present in the cache" in {

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(Some(BusinessDetails().contactingYou(ContactingYou(email = Some(email))))))

        service.getEmailAddress(credId).futureValue mustBe Some(email)
      }

      "return None" when {

        "email is empty" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(Some(BusinessDetails().contactingYou(ContactingYou()))))

          service.getEmailAddress(credId).futureValue mustBe None
        }

        "contacting you is empty" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(Some(BusinessDetails())))

          service.getEmailAddress(credId).futureValue mustBe None
        }

        "business details is empty" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(None))

          service.getEmailAddress(credId).futureValue mustBe None
        }
      }
    }

    "updateEmailAddress is called" must {

      "update and save the model correctly" in {

        val phone = Some("07123456789")
        val bd = BusinessDetails().contactingYou(ContactingYou(phoneNumber = phone))

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(Some(bd)))

        when(mockCacheConnector.save[BusinessDetails](
          eqTo(credId),
          eqTo(BusinessDetails.key),
          eqTo(bd.contactingYou(ContactingYou(phoneNumber = phone, email = Some(email))))
        )(any())
        ).thenReturn(Future.successful(mockCacheMap))

        service.updateEmailAddress(credId, ContactingYouEmail(email)).futureValue mustBe Some(mockCacheMap)

        verify(mockCacheConnector).save[BusinessDetails](
          eqTo(credId),
          eqTo(BusinessDetails.key),
          eqTo(bd.contactingYou(ContactingYou(phoneNumber = phone, email = Some(email))))
        )(any())
      }

      "not update the model" when {

        "business details is not present in cache" in {

          when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
            .thenReturn(Future.successful(None))

          service.updateEmailAddress(credId, ContactingYouEmail(email)).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessDetails](any(), any(), any())(any())
        }
      }
    }
  }
}
