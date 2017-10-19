/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.aboutthebusiness

import connectors.BusinessMatchingConnector
import models.Country
import models.aboutthebusiness.{AboutTheBusiness, CorporationTaxRegisteredYes}
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LimitedCompany, SoleProprietor, UnincorporatedBody}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

import scala.concurrent.Future

class CorporationTaxRegisteredControllerSpec extends GenericTestHelper with MockitoSugar with ScalaFutures with DependencyMocks {

  trait Fixture extends AuthorisedFixture {
    self =>
    val request = addToken(authRequest)

    mockCacheFetchAll
    mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(Some(ReviewDetails(
      "BusinessName",
      Some(LimitedCompany),
      Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
    ))), BusinessMatching.key)

    val controller = new CorporationTaxRegisteredController {
      override val dataCacheConnector = mockCacheConnector
      override val authConnector = self.authConnector
      override val businessMatchingConnector = mock[BusinessMatchingConnector]
    }
  }

  "CorporationTaxRegisteredController" when {

    "get is called" must {

      "display the registered for corporation tax page with pre populated data" in new Fixture {

        val data = AboutTheBusiness(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1111111111")))

        mockCacheGetEntry[AboutTheBusiness](Some(data), AboutTheBusiness.key)

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(true)
        document.getElementById("corporationTaxReference").`val` must be("1111111111")
      }

      "display an empty form when no previous entry" in new Fixture {

        val data = AboutTheBusiness(corporationTaxRegistered = None)

        mockCacheGetEntry[AboutTheBusiness](Some(data), AboutTheBusiness.key)

        val result = controller.get()(request)

        status(result) must be(OK)

        val content = contentAsString(result)

        content must include(Messages("aboutthebusiness.registeredforcorporationtax.title"))

        val document = Jsoup.parse(content)
        document.getElementById("registeredForCorporationTax-true").hasAttr("checked") must be(false)
        document.getElementById("corporationTaxReference").`val` must be("")
      }

      "respond with NOT_FOUND" must {
         "business type is UnincorporatedBody" in new Fixture {

           mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(Some(ReviewDetails(
             "BusinessName",
             Some(UnincorporatedBody),
             Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
           ))), BusinessMatching.key)

           val data = AboutTheBusiness(corporationTaxRegistered = Some(CorporationTaxRegisteredYes("1111111111")))

           mockCacheGetEntry[AboutTheBusiness](Some(data), AboutTheBusiness.key)

           val result = controller.get()(request)
           status(result) must be(NOT_FOUND)
        }
      }

    }

    "post is called" when {

      "with valid data" must {
        "redirect to registered office page" when {
          "edit is false" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForCorporationTax" -> "true",
              "corporationTaxReference" -> "1111111111"
            )

            when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.ConfirmRegisteredOfficeController.get().url))
          }
        }

        "redirect to summary page" when {
          "edit is true" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForCorporationTax" -> "true",
              "corporationTaxReference" -> "1111111111"
            )

            when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
              (any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.aboutthebusiness.routes.SummaryController.get().url))
          }
        }

      }

      "with invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "registeredForCorporationTax" -> "true",
            "corporationTaxReference" -> "ABCDEF"
          )

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

        }
      }

      "business type is SoleProprietor" must {
        "respond with NOT_FOUND" in new Fixture {

          mockCacheGetEntry[BusinessMatching](Some(BusinessMatching(Some(ReviewDetails(
            "BusinessName",
            Some(SoleProprietor),
            Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")
          ))), BusinessMatching.key)

          val newRequest = request.withFormUrlEncodedBody(
            "registeredForCorporationTax" -> "true",
            "corporationTaxReference" -> "1111111111"
          )

          when(controller.dataCacheConnector.save[AboutTheBusiness](any(), any())
            (any(), any(), any())).thenReturn(Future.successful(mockCacheMap))

          val result = controller.post()(newRequest)
          status(result) must be(NOT_FOUND)
        }
      }

    }

  }

}
