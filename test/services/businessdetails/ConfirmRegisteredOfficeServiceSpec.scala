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
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessdetails.{BusinessDetails, ConfirmRegisteredOffice, RegisteredOfficeUK}
import models.businessmatching.BusinessMatching
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class ConfirmRegisteredOfficeServiceSpec extends AmlsSpec with BeforeAndAfterEach {

  val mockCacheConnector = mock[DataCacheConnector]
  val mockCacheMap       = mock[Cache]

  val service = new ConfirmRegisteredOfficeService(mockCacheConnector)

  val credId = "123456"

  val line1    = "Address Line 1"
  val line2    = "Address Line 2"
  val postcode = "AA1 2QQ"
  val office   = RegisteredOfficeUK(line1, Some(line2), postCode = postcode)
  val address  = Address(line1, Some(line2), None, None, Some(postcode), Country("United Kingdom", "UK"))

  "ConfirmRegisteredOfficeService" when {

    "hasRegisteredAddress is called" must {

      "return true if registered office is defined" in {

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(Some(BusinessDetails().registeredOffice(office))))

        service.hasRegisteredAddress(credId).futureValue mustBe Some(true)
      }

      "return false if registered office is not defined" in {

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(Some(BusinessDetails())))

        service.hasRegisteredAddress(credId).futureValue mustBe Some(false)
      }

      "return none if business details cannot be retrieved" in {

        when(mockCacheConnector.fetch[BusinessDetails](eqTo(credId), eqTo(BusinessDetails.key))(any()))
          .thenReturn(Future.successful(None))

        service.hasRegisteredAddress(credId).futureValue mustBe None
      }
    }

    "getAddress is called" must {

      "return the address if it is present in the business matching" in {

        when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Future.successful(
              Some(
                BusinessMatching().reviewDetails(
                  ReviewDetails("Big Corp Inc.", None, address, "12798rg2e8yfg")
                )
              )
            )
          )

        service.getAddress(credId).futureValue mustBe Some(address)
      }

      "return None" when {

        "review details is empty" in {

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(Some(BusinessMatching())))

          service.getAddress(credId).futureValue mustBe None
        }

        "business matching is empty" in {

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(None))

          service.getAddress(credId).futureValue mustBe None
        }
      }
    }

    "updateRegisteredOfficeAddress is called" must {

      "update and save the model correctly" in {

        when(mockCacheConnector.fetchAll(eqTo(credId)))
          .thenReturn(Future.successful(Some(mockCacheMap)))

        when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
          .thenReturn(
            Some(
              BusinessMatching().reviewDetails(
                ReviewDetails("Big Corp Inc.", None, address, "12798rg2e8yfg")
              )
            )
          )

        when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
          .thenReturn(Some(BusinessDetails()))

        when(
          mockCacheConnector.save[BusinessDetails](
            eqTo(credId),
            eqTo(BusinessDetails.key),
            eqTo(BusinessDetails().registeredOffice(office))
          )(any())
        )
          .thenReturn(Future.successful(mockCacheMap))

        service.updateRegisteredOfficeAddress(credId, ConfirmRegisteredOffice(true)).futureValue mustBe Some(office)

        verify(mockCacheConnector).save[BusinessDetails](
          eqTo(credId),
          eqTo(BusinessDetails.key),
          eqTo(BusinessDetails().registeredOffice(office))
        )(any())
      }

      "not update or save the model" when {

        "user has answered false" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Some(
                BusinessMatching().reviewDetails(
                  ReviewDetails("Big Corp Inc.", None, address, "12798rg2e8yfg")
                )
              )
            )

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(Some(BusinessDetails()))

          service.updateRegisteredOfficeAddress(credId, ConfirmRegisteredOffice(false)).futureValue mustBe None
        }

        "business details is empty" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(
              Some(
                BusinessMatching().reviewDetails(
                  ReviewDetails("Big Corp Inc.", None, address, "12798rg2e8yfg")
                )
              )
            )

          when(mockCacheMap.getEntry[BusinessDetails](eqTo(BusinessDetails.key))(any()))
            .thenReturn(None)

          service.updateRegisteredOfficeAddress(credId, ConfirmRegisteredOffice(true)).futureValue mustBe None
        }

        "business matching is empty" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          when(mockCacheMap.getEntry[BusinessMatching](eqTo(BusinessMatching.key))(any()))
            .thenReturn(None)

          service.updateRegisteredOfficeAddress(credId, ConfirmRegisteredOffice(true)).futureValue mustBe None
        }

        "cache cannot be retrieved" in {

          when(mockCacheConnector.fetchAll(eqTo(credId)))
            .thenReturn(Future.successful(None))

          service.updateRegisteredOfficeAddress(credId, ConfirmRegisteredOffice(true)).futureValue mustBe None
        }
      }
    }
  }
}
