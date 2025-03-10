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

package services.businessmatching

import connectors.DataCacheConnector
import models.Country
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessType.LPrLLP
import models.businessmatching.{BusinessMatching, BusinessType}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import services.cache.Cache
import utils.AmlsSpec

import scala.concurrent.Future

class BusinessTypeServiceSpec
    extends AmlsSpec
    with ScalaCheckPropertyChecks
    with BeforeAndAfterEach
    with IntegrationPatience {

  val mockCacheConnector = mock[DataCacheConnector]

  val service = new BusinessTypeService(mockCacheConnector)

  val credId = "123456"

  override protected def beforeEach(): Unit = reset(mockCacheConnector)

  "BusinessTypeService" when {

    "getBusinessType" must {

      "return the business type" when {

        "it is set in the cache" in {

          forAll(Gen.oneOf(BusinessType.all)) { businessType =>
            val bm = BusinessMatching().reviewDetails(
              ReviewDetails(
                "Big Corp",
                Some(businessType),
                Address(
                  "line1",
                  Some("line2"),
                  Some("line3"),
                  Some("line4"),
                  Some("AA11 1AA"),
                  Country("United Kingdom", "GB")
                ),
                "wdvhibsd9vh823fwdj"
              )
            )
            when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
              .thenReturn(Future.successful(Some(bm)))

            service.getBusinessType(credId).futureValue mustBe Some(businessType)
          }
        }
      }

      "return none" when {

        "business type is not in cache" in {

          val bm = BusinessMatching().reviewDetails(
            ReviewDetails(
              "Big Corp",
              None,
              Address(
                "line1",
                Some("line2"),
                Some("line3"),
                Some("line4"),
                Some("AA11 1AA"),
                Country("United Kingdom", "GB")
              ),
              "wdvhibsd9vh823fwdj"
            )
          )
          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(Some(bm)))

          service.getBusinessType(credId).futureValue mustBe None
        }

        "review details is not in cache" in {

          val bm = BusinessMatching()

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(Some(bm)))

          service.getBusinessType(credId).futureValue mustBe None
        }

        "business matching is not in cache" in {

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(None))

          service.getBusinessType(credId).futureValue mustBe None
        }
      }
    }

    "updateBusinessType" must {

      "update and save the model correctly" in {

        val reviewDetails = ReviewDetails(
          "Big Corp",
          None,
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "wdvhibsd9vh823fwdj"
        )

        lazy val oldModel = BusinessMatching().reviewDetails(reviewDetails)

        lazy val newModel = oldModel.reviewDetails(reviewDetails.copy(businessType = Some(LPrLLP)))

        when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
          .thenReturn(Future.successful(Some(oldModel)))

        when(mockCacheConnector.save[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key), eqTo(newModel))(any()))
          .thenReturn(Future.successful(mock[Cache]))

        service.updateBusinessType(credId, LPrLLP).futureValue mustBe Some(LPrLLP)

        verify(mockCacheConnector).save[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key), eqTo(newModel))(
          any()
        )
      }

      "not update or save the model" when {

        "review details is not set in cache" in {

          val bm = BusinessMatching()

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(Some(bm)))

          service.updateBusinessType(credId, LPrLLP).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessMatching](any(), any(), any())(any())
        }

        "business matching is not set in cache" in {

          when(mockCacheConnector.fetch[BusinessMatching](eqTo(credId), eqTo(BusinessMatching.key))(any()))
            .thenReturn(Future.successful(None))

          service.updateBusinessType(credId, LPrLLP).futureValue mustBe None

          verify(mockCacheConnector, times(0))
            .save[BusinessMatching](any(), any(), any())(any())
        }
      }
    }
  }
}
