/*
 * Copyright 2019 HM Revenue & Customs
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

import connectors.DataCacheConnector
import models.Country
import models.aboutthebusiness._
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessType.{LPrLLP, LimitedCompany, Partnership, UnincorporatedBody}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class VATRegisteredControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>
    val request = addToken(authRequest)

    val controller = new VATRegisteredController (
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector
    )
  }

  "BusinessRegisteredForVATController" when {

    "get is called" must {

      "display the registered for VAT page" in new Fixture {

        when{
          controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())
        } thenReturn Future.successful(None)

        val result = controller.get()(request)

        status(result) must be(OK)
        contentAsString(result) must include(Messages("businessdetails.registeredforvat.title"))
      }

      "display the registered for VAT page with pre populated data" in new Fixture {

        when {
          controller.dataCacheConnector.fetch[AboutTheBusiness](any())(any(), any(), any())
        } thenReturn Future.successful(Some(AboutTheBusiness(Some(PreviouslyRegisteredYes("")), None, Some(VATRegisteredYes("123456789")))))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("input[value=true]").hasAttr("checked") must be(true)
      }

    }

    "post is called" when {

      "with valid data" must {

        "redirect to RegisteredOfficeController" when {
          "customer is a Partnership" in new Fixture {

            val partnership = ReviewDetails("BusinessName", Some(Partnership),
              Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

            mockCacheGetEntry(Some(BusinessMatching(Some(partnership))), BusinessMatching.key)
            mockCacheUpdate(Some(AboutTheBusiness.key), AboutTheBusiness())

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessdetails.routes.ConfirmRegisteredOfficeController.get().url))
          }
        }

        "redirect to CorporationTaxRegistered" when {
          "customer is a LLP" in new Fixture {

            val llp = ReviewDetails("BusinessName", Some(LPrLLP),
              Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

            mockCacheGetEntry(Some(BusinessMatching(Some(llp))), BusinessMatching.key)
            mockCacheUpdate(Some(AboutTheBusiness.key), AboutTheBusiness())

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessdetails.routes.CorporationTaxRegisteredController.get().url))
          }

          "customer is a Limited Company" in new Fixture {

            val details = ReviewDetails("BusinessName", Some(LimitedCompany),
              Address("line1", "line2", Some("line3"), Some("line4"), Some("AA11 1AA"), Country("United Kingdom", "GB")), "ghghg")

            mockCacheGetEntry(Some(BusinessMatching(Some(details))), BusinessMatching.key)
            mockCacheUpdate(Some(AboutTheBusiness.key), AboutTheBusiness())

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789"
            )

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessdetails.routes.CorporationTaxRegisteredController.get().url))
          }
        }

        "redirect to SummaryController" when {
          "in edit" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "registeredForVAT" -> "true",
              "vrnNumber" -> "123456789"
            )

            val partnership = ReviewDetails("BusinessName", Some(LPrLLP),
              Address("line1", "line2", Some("line3"), Some("line4"), Some("NE77 0QQ"), Country("United Kingdom", "GB")), "ghghg")

            mockCacheGetEntry(Some(BusinessMatching(Some(partnership))), BusinessMatching.key)
            mockCacheUpdate(Some(AboutTheBusiness.key), AboutTheBusiness())

            val result = controller.post(true)(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.businessdetails.routes.SummaryController.get().url))
          }
        }

      }

      "with invalid data" must {
        "respond with BAD_REQUEST" in new Fixture {

          val newRequest = request.withFormUrlEncodedBody(
            "registeredForVATYes" -> "1234567890"
          )

          when(controller.dataCacheConnector.fetchAll(any[HeaderCarrier], any[AuthContext]))
            .thenReturn(Future.successful(Some(mockCacheMap)))

          val result = controller.post()(newRequest)
          status(result) must be(BAD_REQUEST)

          contentAsString(result) must include(Messages("error.required.atb.registered.for.vat"))
        }
      }

    }
  }

}


