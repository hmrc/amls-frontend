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

package controllers.asp

import controllers.actions.SuccessfulAuthAction
import forms.asp.ServicesOfBusinessFormProvider
import models.asp._
import models.asp.Service._
import models.businessmatching.BusinessActivity.AccountancyServices
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.cache.Cache
import utils.{AmlsSpec, DependencyMocks}
import views.html.asp.ServicesOfBusinessView

import scala.concurrent.Future

class ServicesOfBusinessControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  val emptyCache: Cache = Cache.empty

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    lazy val servicesOfBusiness: ServicesOfBusinessView = inject[ServicesOfBusinessView]
    val controller                                      = new ServicesOfBusinessController(
      mockCacheConnector,
      mockStatusService,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mockServiceFlow,
      mockMcc,
      inject[ServicesOfBusinessFormProvider],
      servicesOfBusiness
    )

    mockCacheFetch[Asp](None)
    mockCacheSave[Asp]
    mockIsNewActivityNewAuth(value = false)
  }

  "ServicesOfBusinessController" must {

    "on get display Which services does your business provide page" in new Fixture {

      val result: Future[Result] = controller.get()(request)
      status(result)          must be(OK)
      contentAsString(result) must include(Messages("asp.services.title"))
    }

    "submit with valid data" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
          .withFormUrlEncodedBody(
            "services[1]" -> BookKeeping.toString,
            "services[2]" -> Accountancy.toString
          )

      mockApplicationStatus(SubmissionDecisionRejected)

      val result: Future[Result] = controller.post()(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.OtherBusinessTaxMattersController.get().url))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {

      mockCacheFetch(Some(Asp(Some(ServicesOfBusiness(Set(BookKeeping, Accountancy))), None)))

      val result: Future[Result] = controller.get()(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select(s"input[value=${BookKeeping.toString}]").hasAttr("checked") must be(true)
      document.select(s"input[value=${Accountancy.toString}]").hasAttr("checked") must be(true)
    }

    "fail submission on invalid value" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
          .withFormUrlEncodedBody(
            "services[1]" -> "foo"
          )

      val result: Future[Result] = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").text() must include(
        messages("error.required.asp.business.services")
      )
    }

    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
          .withFormUrlEncodedBody(
            "services[1]" -> ""
          )

      val result: Future[Result] = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary").text() must include(
        messages("error.required.asp.business.services")
      )
    }

    "submit with valid data in edit mode" in new Fixture {

      val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
        FakeRequest(POST, routes.ServicesOfBusinessController.post(true).url)
          .withFormUrlEncodedBody(
            "services[1]" -> BookKeeping.toString,
            "services[2]" -> Accountancy.toString,
            "services[3]" -> Auditing.toString
          )

      mockApplicationStatus(SubmissionDecisionRejected)

      val result: Future[Result] = controller.post(true)(newRequest)
      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.SummaryController.get.url))
    }

    "go to the date of change page" when {
      "the submission has been approved and registeredOffice has changed" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
            .withFormUrlEncodedBody(
              "services[1]" -> BookKeeping.toString,
              "services[2]" -> Accountancy.toString,
              "services[3]" -> Auditing.toString
            )
        val result: Future[Result]                              = controller.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get.url))
      }
    }

    "go to the date of change page" when {
      "status is ready for renewal and services selection has changed" in new Fixture {

        mockApplicationStatus(ReadyForRenewal(None))

        val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
          FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
            .withFormUrlEncodedBody(
              "services[1]" -> BookKeeping.toString,
              "services[2]" -> Accountancy.toString,
              "services[3]" -> Auditing.toString
            )
        val result: Future[Result]                              = controller.post()(newRequest)

        status(result)           must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get.url))
      }
    }

    "Calling POST" when {
      "the status is approved" when {
        "the service has just been added" must {
          "redirect to the next page in the flow" in new Fixture {
            val newRequest: FakeRequest[AnyContentAsFormUrlEncoded] =
              FakeRequest(POST, routes.ServicesOfBusinessController.post().url)
                .withFormUrlEncodedBody(
                  "services[1]" -> BookKeeping.toString,
                  "services[2]" -> Accountancy.toString,
                  "services[3]" -> Auditing.toString
                )

            mockApplicationStatus(SubmissionDecisionApproved)
            mockIsNewActivityNewAuth(value = true, Some(AccountancyServices))

            val result: Future[Result] = controller.post()(newRequest)
            status(result)           must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.asp.routes.OtherBusinessTaxMattersController.get().url))
          }
        }
      }
    }

  }

}
