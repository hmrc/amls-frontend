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

package controllers.businessdetails

import connectors.BusinessMatchingConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.businessdetails.{BusinessDetails, CorporationTaxRegisteredYes}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.{BusinessMatching, BusinessType}
import models.businessmatching.BusinessType.{LimitedCompany, UnincorporatedBody}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers._
import utils.DependencyMocks
import utils.AmlsSpec
import org.mockito.ArgumentMatchers.{eq => eqTo}

class CorporationTaxRegisteredControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures with DependencyMocks {

  trait Fixture {
    self =>

    val request = addToken(authRequest)

    val reviewDetails    = ReviewDetails(
      "BusinessName",
      Some(LimitedCompany),
      Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")),
      "ghghg",
      Some("sdsw")
    )
    val businessMatching = BusinessMatching(Some(reviewDetails))

    mockCacheFetchAll
    mockCacheGetEntry[BusinessMatching](Some(businessMatching), BusinessMatching.key)
    mockCacheSave[BusinessDetails]

    val controller = new CorporationTaxRegisteredController(
      dataCacheConnector = mockCacheConnector,
      businessMatchingConnector = mock[BusinessMatchingConnector],
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      cc = mockMcc,
      errorView
    )
  }

  "CorporationTaxRegisteredController" when {

    "get is called" must {

      "redirect to ConfirmRegisteredOfficeController" in new Fixture {

        val data = BusinessDetails(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1111111111")))

        mockCacheGetEntry[BusinessDetails](Some(data), BusinessDetails.key)

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) must be(Some(routes.ConfirmRegisteredOfficeController.get().url))
      }
    }

    "process the UTR" when {
      "business matching UTR exists" in new Fixture {
        val reviewDtlsUtr = ReviewDetails(
          "BusinessName",
          Some(BusinessType.LimitedCompany),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "XE0000000000000",
          Some("1111111111")
        )

        val corpTax = CorporationTaxRegisteredYes(reviewDtlsUtr.utr.get)

        override val businessMatching = BusinessMatching(Some(reviewDtlsUtr))

        mockCacheFetchAll
        mockCacheGetEntry[BusinessMatching](Some(businessMatching), BusinessMatching.key)
        mockCacheSave[BusinessDetails]

        val data = BusinessDetails(corporationTaxRegistered = None)

        mockCacheGetEntry[BusinessDetails](Some(data), BusinessDetails.key)

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)

        verify(controller.dataCacheConnector)
          .save(eqTo("internalId"), eqTo(BusinessDetails.key), eqTo(data.corporationTaxRegistered(corpTax)))(any())
      }

      "business matching UTR NOT exists" in new Fixture {
        val reviewDtlsUtr = ReviewDetails(
          "BusinessName",
          Some(BusinessType.LimitedCompany),
          Address(
            "line1",
            Some("line2"),
            Some("line3"),
            Some("line4"),
            Some("AA11 1AA"),
            Country("United Kingdom", "GB")
          ),
          "XE0000000000000",
          None
        )

        override val businessMatching = BusinessMatching(Some(reviewDtlsUtr))

        mockCacheFetchAll
        mockCacheGetEntry[BusinessMatching](Some(businessMatching), BusinessMatching.key)
        mockCacheSave[BusinessDetails]

        val data = BusinessDetails(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1111111111")))

        mockCacheGetEntry[BusinessDetails](Some(data), BusinessDetails.key)

        val result = controller.get()(request)
        status(result) must be(SEE_OTHER)
      }
    }

    "respond with NOT_FOUND" must {
      "business type is UnincorporatedBody" in new Fixture {

        mockCacheGetEntry[BusinessMatching](
          Some(
            BusinessMatching(
              Some(
                ReviewDetails(
                  "BusinessName",
                  Some(UnincorporatedBody),
                  Address(
                    "line1",
                    Some("line2"),
                    Some("line3"),
                    Some("line4"),
                    Some("AA11 1AA"),
                    Country("United Kingdom", "GB")
                  ),
                  "ghghg"
                )
              )
            )
          ),
          BusinessMatching.key
        )

        val data = BusinessDetails(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1111111111")))

        mockCacheGetEntry[BusinessDetails](Some(data), BusinessDetails.key)

        val result = controller.get()(request)
        status(result) must be(NOT_FOUND)
      }
    }
  }
}
