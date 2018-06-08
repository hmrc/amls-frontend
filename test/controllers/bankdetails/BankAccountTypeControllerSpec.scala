/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.matchers.Matcher
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec, StatusConstants}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class BankAccountTypeControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    val request = addToken(authRequest)

    val injector = new GuiceInjectorBuilder()
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))
      .overrides(bind[AuditConnector].to(mock[AuditConnector]))
      .build()

    lazy val controller = injector.instanceOf[BankAccountTypeController]

  }

  "BankAccountTypeController" when {
    "get:" must {
      "respond with OK and display the blank 'bank account type' page" when {
        "there is any number of bank accounts" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(
              BankDetails(None, None, status = Some(StatusConstants.Deleted)),
              BankDetails(Some(NoBankAccountUsed), None, status = Some(StatusConstants.Added))
            )))
          mockApplicationStatus(SubmissionReady)

          val result = controller.get(1, false)(request)

          status(result) must be(OK)
          contentAsString(result) must include(Messages("bankdetails.accounttype.title"))
          val document = Jsoup.parse(contentAsString(result))

          document.select("input[type=radio][name=bankAccountType][value=01]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType][value=02]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType][value=03]").hasAttr("checked") must be(false)
          document.select("input[type=radio][name=bankAccountType]").size() must be(3)
        }

        "there is already a bank account type" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockApplicationStatus(SubmissionReady)

          val result = controller.get(1)(request)
          val document = Jsoup.parse(contentAsString(result)).select("input[value=01]").hasAttr("checked")

          status(result) must be(OK)
          document must be(true)
        }
      }

      "respond with NOT_FOUND" when {
        "there is no bank account information at all" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](None)
          mockApplicationStatus(SubmissionReady)

          val result = controller.get(1, false)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an amendment" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount)), BankDetails(Some(PersonalAccount)))))
          mockApplicationStatus(SubmissionReadyForReview)

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)
        }
        "editing an variation" in new Fixture {

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount)), BankDetails(Some(PersonalAccount)))))
          mockApplicationStatus(SubmissionDecisionApproved)

          val result = controller.get(1, true)(request)

          status(result) must be(NOT_FOUND)
        }

      }
    }

    "post is called" must {
      "resopnd with OK and redirect to the bank account details page" when {

        "not editing and there is valid account type" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1, false)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountNameController.get(1, false).url))
        }

          "not editing and there is no bank account" in new Fixture {

            val newRequest = request.withFormUrlEncodedBody(
              "bankAccountType" -> "04"
            )

            mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))))
            mockCacheSave[Seq[BankDetails]]

            val result = controller.post(1, false)(newRequest)

            status(result) must be(SEE_OTHER)
            redirectLocation(result) must be(Some(routes.SummaryController.get(false).url))
        }

        "editing and there is valid account type but no account details" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(Some(PersonalAccount), None))))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountNameController.get(1, true).url))
        }

        "editing and there is both a valid account type and valid account details" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "01"
          )

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(
            Some(PersonalAccount),
            Some("AccountName"),
            Some(UKAccount("12341234", "000000"))
          ))))

          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(1, true)(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(routes.BankAccountNameController.get(1, true).url))
        }
      }

      "respond with BAD_REQUEST" when {
        "there is invalid data" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "10"
          )

          mockCacheFetch[Seq[BankDetails]](None)
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(0, false)(newRequest)
          val document = Jsoup.parse(contentAsString(result)).select("span").html()

          status(result) must be(BAD_REQUEST)
          document must include("Invalid value")
        }
      }

      "respond with NOT_FOUND" when {
        "the given index is out of bounds" in new Fixture {
          val newRequest = request.withFormUrlEncodedBody(
            "bankAccountType" -> "04"
          )

          mockApplicationStatus(SubmissionDecisionApproved)

          mockCacheFetch[Seq[BankDetails]](Some(Seq(BankDetails(None, None))), Some(BankDetails.key))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post(3, false)(newRequest)

          status(result) must be(NOT_FOUND)
        }
      }
    }
  }
}
