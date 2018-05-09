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

import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.mockito.Matchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

import scala.collection.JavaConversions._

class SummaryControllerSpec extends AmlsSpec with MockitoSugar {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self => val request = addToken(authRequest)

    val controller = new SummaryController(
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector,
      statusService = mockStatusService
    )
  }

  "Get" must {

    "load the summary page with the correct link text when the section is incomplete" in new Fixture {

      val model = BankDetails(None, None)

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
      mockApplicationStatus(SubmissionReady)

      val result = controller.get()(request)

      status(result) must be(OK)
      contentAsString(result) must include("Accept and complete section")
      contentAsString(result) mustNot include("Confirm and continue")
    }


    "load the summary page with the correct link text when the section is complete" in new Fixture {

      val model = BankDetails(None, None, hasAccepted = true)

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
      mockApplicationStatus(SubmissionReady)

      val result = controller.get(true)(request)

      status(result) must be(OK)
      contentAsString(result) must include("Confirm and continue")
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      mockCacheFetch[Seq[BankDetails]](None)
      mockApplicationStatus(SubmissionReady)

      val result = controller.get()(request)

      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "show bank account details on the Check your Answers page" in new Fixture {

      val model = BankDetails(
        Some(PersonalAccount),
        Some("Account Name"),
        Some(UKAccount("12341234","000000"))
      )

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
      mockApplicationStatus(SubmissionReady)

      val result = controller.get()(request)

      val contentString = contentAsString(result)

      val document = Jsoup.parse(contentString)

      val pageTitle = Messages("title.cya") + " - " +
        Messages("summary.bankdetails") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      document.title() mustBe pageTitle

      contentString must include("Account Name")
      contentString must include("Account number: 12341234")
      contentString must include("Sort code: 00-00-00")
      contentString must include("UK Bank Account")
      contentString must include("A personal bank account")
    }

    "show no bank account text" when {
      "no bank account is selected" in new Fixture {

        val model = BankDetails(None,None)

        mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
        mockApplicationStatus(SubmissionReady)

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("bankdetails.summary.nobank.account"))
      }
    }

    "not show edit links" when {
      "on check-your-answers page in amendments" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some("Account Name"),
          Some(UKAccount("12341234","000000"))
        )

        mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
        mockApplicationStatus(SubmissionReadyForReview)

        val result = controller.get()(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)

        val pageTitle = Messages("title.cya") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on check-your-answers page in variations" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some("Account Name"),
          Some(UKAccount("12341234","000000"))
        )

        mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get()(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)

        val pageTitle = Messages("title.cya") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on your-answers page in amendments" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some("Account Name"),
          Some(UKAccount("12341234","000000"))
        )

        mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
        mockApplicationStatus(SubmissionReadyForReview)

        val result = controller.get(true)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        val pageTitle = Messages("title.ya") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on your-answers page in variations" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some("Account Name"),
          Some(UKAccount("12341234","000000"))
        )

        mockCacheFetch[Seq[BankDetails]](Some(Seq(model)))
        mockApplicationStatus(SubmissionDecisionApproved)

        val result = controller.get(true)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        val pageTitle = Messages("title.ya") + " - " +
          Messages("summary.bankdetails") + " - " +
          Messages("title.amls") + " - " + Messages("title.gov")

        document.title() mustBe pageTitle

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }

    }
  }

  "post is called" must {
    "respond with OK and redirect to the bank account details page" when {

      "all questions are complete" in new Fixture {

        val newRequest = request.withFormUrlEncodedBody("hasAccepted" -> "true")

        mockCacheFetch[Seq[BankDetails]](None)
        mockCacheSave[Seq[BankDetails]]

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

      "when there are bank accounts" when {

        "update the accepted flag" in new Fixture {
          val accountType1 = PersonalAccount
          val bankAccount1 = UKAccount("111111", "11-11-11")

          val accountType2 = PersonalAccount
          val bankAccount2 = UKAccount("222222", "22-22-22")

          val accountType3 = PersonalAccount
          val bankAccount3 = UKAccount("333333", "33-33-33")

          val accountType4 = PersonalAccount
          val bankAccount4 = UKAccount("444444", "44-44-44")

          val Model1 = BankDetails(Some(accountType1), None, Some(bankAccount1))
          val Model2 = BankDetails(Some(accountType2), None, Some(bankAccount2))
          val Model3 = BankDetails(Some(accountType3), None, Some(bankAccount3))
          val Model4 = BankDetails(Some(accountType4), None, Some(bankAccount4))

          val completeModel1 = BankDetails(Some(accountType1), None, Some(bankAccount1), hasAccepted = true)
          val completeModel2 = BankDetails(Some(accountType2), None, Some(bankAccount2), hasAccepted = true)
          val completeModel3 = BankDetails(Some(accountType3), None, Some(bankAccount3), hasAccepted = true)
          val completeModel4 = BankDetails(Some(accountType4), None, Some(bankAccount4), hasAccepted = true)

          val bankAccounts = Seq(Model1,Model2,Model3,Model4)

          val newRequest = request.withFormUrlEncodedBody("hasAccepted" -> "true")

          mockCacheFetch[Seq[BankDetails]](Some(bankAccounts))
          mockCacheSave[Seq[BankDetails]]

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

          verify(controller.dataCacheConnector).save[Seq[BankDetails]](any(),
            meq(Seq(completeModel1, completeModel2,completeModel3,completeModel4)))(any(), any(), any())
        }

      }
    }
  }
}
