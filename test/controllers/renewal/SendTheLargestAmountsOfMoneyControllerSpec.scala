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

package controllers.renewal

import cats.implicits._
import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import forms.renewal.SendLargestAmountsOfMoneyFormProvider
import models.Country
import models.renewal.{CustomersOutsideUK, Renewal, SendTheLargestAmountsOfMoney}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, PatienceConfiguration}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.{RenewalService, StatusService}
import services.cache.Cache
import utils.{AmlsSpec, AutoCompleteServiceMocks}
import views.html.renewal.SendLargestAmountsOfMoneyView

import scala.concurrent.Future

class SendTheLargestAmountsOfMoneyControllerSpec
    extends AmlsSpec
    with MockitoSugar
    with PatienceConfiguration
    with IntegrationPatience
    with Injecting {

  trait Fixture extends AutoCompleteServiceMocks {
    self =>
    val request = addToken(authRequest)

    val mockCacheMap = mock[Cache]

    val emptyCache = Cache.empty

    lazy val mockDataCacheConnector = mock[DataCacheConnector]
    lazy val mockStatusService      = mock[StatusService]
    lazy val mockRenewalService     = mock[RenewalService]
    lazy val view                   = inject[SendLargestAmountsOfMoneyView]
    val controller                  = new SendTheLargestAmountsOfMoneyController(
      dataCacheConnector = mockDataCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      renewalService = mockRenewalService,
      cc = mockMcc,
      autoCompleteService = mockAutoComplete,
      formProvider = inject[SendLargestAmountsOfMoneyFormProvider],
      view = view
    )
  }

  trait FormSubmissionFixture extends Fixture {
    def formData(valid: Boolean)    = if (valid) "largestAmountsOfMoney[0]" -> "GB" else "largestAmountsOfMoney[0]" -> ""
    def formRequest(valid: Boolean) =
      FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
        .withFormUrlEncodedBody(formData(valid))

    when(mockRenewalService.getRenewal(any())).thenReturn(Future.successful(None))

    when(mockRenewalService.updateRenewal(any(), any())).thenReturn(Future.successful(emptyCache))

    def post(edit: Boolean = false, valid: Boolean = true)(block: Future[Result] => Unit) =
      block(controller.post(edit)(formRequest(valid)))
  }

  val emptyCache = Cache.empty

  "SendTheLargestAmountsOfMoneyController" when {

    "get is called" must {
      "load the 'Where to Send The Largest Amounts Of Money' page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.title() must be(
          messages("renewal.msb.largest.amounts.title") + " - " + messages("summary.renewal") + " - " + messages(
            "title.amls"
          ) + " - " + messages("title.gov")
        )
      }

      "pre-populate the 'Where to Send The Largest Amounts Of Money' Page" in new Fixture {

        when(controller.dataCacheConnector.fetch[Renewal](any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                Renewal(sendTheLargestAmountsOfMoney =
                  Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB"))))
                )
              )
            )
          )

        val result = controller.get()(request)
        status(result) must be(OK)

        val document = Jsoup.parse(contentAsString(result))
        document.select("select[name=largestAmountsOfMoney[0]] > option[value=GB]").hasAttr("selected") must be(true)
      }
    }

    "post is called" when {
      "edit is false" must {
        "redirect to the MostTransactionsController with valid data" in new FormSubmissionFixture {
          post() { result =>
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(routes.MostTransactionsController.get().url.some)
          }
        }
      }

      "edit is true" must {
        "redirect to the SummaryController" in new FormSubmissionFixture {
          post(edit = true) { result =>
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(routes.SummaryController.get.url.some)
          }
        }
        "redirect to SendTheLargestAmountsOfMoneyController" when {
          "CustomersOutsideUK is contains countries" when {
            "MostTransactions is None" in new FormSubmissionFixture {

              when(mockRenewalService.getRenewal(any()))
                .thenReturn(
                  Future.successful(
                    Some(
                      Renewal(
                        customersOutsideUK = Some(CustomersOutsideUK(Some(Seq(Country("GB", "GB"))))),
                        mostTransactions = None
                      )
                    )
                  )
                )

              post(edit = true) { result =>
                status(result) mustBe SEE_OTHER
                redirectLocation(result) mustEqual routes.MostTransactionsController.get(true).url.some
              }
            }
          }
        }
      }

      "given invalid data, must respond with BAD_REQUEST" in new FormSubmissionFixture {

        val newRequest = FakeRequest(POST, routes.SendTheLargestAmountsOfMoneyController.post().url)
          .withFormUrlEncodedBody("largestAmountsOfMoney[0]" -> "")

        val result = controller.post()(newRequest)
        status(result) must be(BAD_REQUEST)
      }
    }
  }
}
