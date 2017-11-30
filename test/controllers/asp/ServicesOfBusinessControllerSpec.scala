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

package controllers.asp

import models.asp._
import models.businessmatching.AccountancyServices
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionDecisionRejected}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.{AuthorisedFixture, DependencyMocks, GenericTestHelper}

class ServicesOfBusinessControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks{
    self => val request = addToken(authRequest)

    val controller = new ServicesOfBusinessController(
      mockCacheConnector,
      mockStatusService,
      self.authConnector,
      mockServiceFlow
    )

    mockCacheFetch[Asp](None)
    mockCacheSave[Asp]
    mockIsNewActivity(false)
  }

  val emptyCache = CacheMap("", Map.empty)

  "ServicesOfBusinessController" must {

    "on get display Which services does your business provide page" in new Fixture {

      val result = controller.get()(request)
      status(result) must be(OK)
      contentAsString(result) must include(Messages("asp.services.title"))
    }

    "submit with valid data" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> "02",
        "services" -> "04"
      )

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post()(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.OtherBusinessTaxMattersController.get().url))
    }

    "load the page with data when the user revisits at a later time" in new Fixture {

      mockCacheFetch(Some(Asp(Some(ServicesOfBusiness(Set(BookKeeping, Accountancy))), None)))

      val result = controller.get()(request)
      status(result) must be(OK)

      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("input[value=03]").hasAttr("checked") must be(true)
      document.select("input[value=01]").hasAttr("checked") must be(true)
    }

    "fail submission on error" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services" -> "0299999"
      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      contentAsString(result) must include("Invalid value")
    }

    "fail submission when no check boxes were selected" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(

      )

      val result = controller.post()(newRequest)
      status(result) must be(BAD_REQUEST)
      val document: Document = Jsoup.parse(contentAsString(result))
      document.select("a[href=#services]").html() must include(Messages("error.required.asp.business.services"))
    }

    "submit with valid data in edit mode" in new Fixture {

      val newRequest = request.withFormUrlEncodedBody(
        "services[1]" -> "02",
        "services[0]" -> "01",
        "services[2]" -> "03"
      )

      mockApplicationStatus(SubmissionDecisionRejected)

      val result = controller.post(true)(newRequest)
      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.asp.routes.SummaryController.get().url))
    }

    "go to the date of change page" when {
      "the submission has been approved and registeredOffice has changed" in new Fixture {

        mockApplicationStatus(SubmissionDecisionApproved)

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "02",
          "services[1]" -> "01",
          "services[2]" -> "03")
        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get().url))
      }
    }

    "go to the date of change page" when {
      "status is ready for renewal and services selection has changed" in new Fixture {

        mockApplicationStatus(ReadyForRenewal(None))

        val newRequest = request.withFormUrlEncodedBody(
          "services[0]" -> "02",
          "services[1]" -> "01",
          "services[2]" -> "03")
        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.asp.routes.ServicesOfBusinessDateOfChangeController.get().url))
      }
    }

    "Calling POST" when {
      "the status is approved" when {
        "the service has just been added" must {
          "redirect to the next page in the flow" in new Fixture {
            val newRequest = request.withFormUrlEncodedBody(
              "services[0]" -> "02",
              "services[1]" -> "01",
              "services[2]" -> "03")

            mockApplicationStatus(SubmissionDecisionApproved)
            mockIsNewActivity(true, Some(AccountancyServices))

            val result = controller.post()(newRequest)
            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(controllers.asp.routes.OtherBusinessTaxMattersController.get().url))
          }
        }
      }
    }

  }

}
