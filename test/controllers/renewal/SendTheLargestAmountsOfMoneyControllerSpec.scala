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

package controllers.renewal

import cats.implicits._
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.Country
import models.renewal.{CustomersOutsideUK, Renewal, SendTheLargestAmountsOfMoney}
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{RenewalService, StatusService}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AmlsSpec, AuthorisedFixture, AutoCompleteServiceMocks}

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyControllerSpec extends AmlsSpec with MockitoSugar with PatienceConfiguration with IntegrationPatience {

  trait Fixture extends AuthorisedFixture with AutoCompleteServiceMocks {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[CacheMap]

    val emptyCache = CacheMap("", Map.empty)

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService = mock[StatusService]
    lazy val mockRenewalService = mock[RenewalService]

    val controller = new SendTheLargestAmountsOfMoneyController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      renewalService = mockRenewalService, cc = mockMcc,
      autoCompleteService = mockAutoComplete
    )
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean) = if (valid) "largestAmountsOfMoney[0]" -> "GB" else "largestAmountsOfMoney[0]" -> ""
    def formRequest(valid: Boolean) = request.withFormUrlEncodedBody(formData(valid))

    when(mockRenewalService.getRenewal(any())(any(), any()))
      .thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any(), any())(any(), any()))
      .thenReturn(Future.successful(emptyCache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Result => Unit) =
      block(await(controller.post(edit)(formRequest(valid))))
  }

  val emptyCache = CacheMap("", Map.empty)

  "SendTheLargestAmountsOfMoneyController" when {

    "get is called" must {
      "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be(Messages("renewal.msb.largest.amounts.title") + " - " + Messages("summary.renewal") + " - " + Messages("title.amls") + " - " + Messages("title.gov"))
      }

      "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(
            Renewal(sendTheLargestAmountsOfMoney = Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))))
          )))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("select[name=largestAmountsOfMoney[0]] > option[value=GB]").hasAttr("selected") must be(true)
      }
    }

    "post is called" when {
      "edit is false" must {
        "redirect to the MostTransactionsController with valid data" in new FormSubmissionFixture {
          post(){ result =>
            result.header.status must be (SEE_OTHER)
            result.header.headers.get("Location")  must be(routes.MostTransactionsController.get().url.some)
          }
        }
      }

      "edit is true" must {
        "redirect to the SummaryController" in new FormSubmissionFixture {
          post(edit = true){ result =>
            result.header.status must be (SEE_OTHER)
            result.header.headers.get("Location")  must be(routes.SummaryController.get().url.some)
          }
        }
        "redirect to SendTheLargestAmountsOfMoneyController" when {
          "CustomersOutsideUK is contains countries" when {
            "MostTransactions is None" in new FormSubmissionFixture {

              when(mockRenewalService.getRenewal(any())(any(), any()))
                .thenReturn(Future.successful(Some(Renewal(
                  customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB","GB"))))),
                  mostTransactions = None
                ))))

              post(edit = true) { result =>
                result.header.status mustBe SEE_OTHER
                result.header.headers.get("Location") mustEqual routes.MostTransactionsController.get(true).url.some
              }
            }
          }
        }
      }

      "given invalid data, must respond with BAD_REQUEST" in new FormSubmissionFixture {

        val newRequest = request.withFormUrlEncodedBody(
          "largestAmountsOfMoney[0]" -> ""
        )

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)

        val document = Jsoup.parse(contentAsString(result))
        document.select("a[href=#largestAmountsOfMoney]").html() must include(Messages("error.required.renewal.largest.amounts.country"))
      }
    }
  }
}
