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

package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import utils.{AuthorisedFixture, GenericTestHelper, StatusConstants}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.collection.JavaConversions._
import scala.concurrent.Future

class SummaryControllerSpec extends GenericTestHelper with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self => val request = addToken(authRequest)

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  "Get" must {

    "load the summary page with the correct link text when the section is incomplete" in new Fixture {

      val model = BankDetails(None, None)

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get(false)(request)

      status(result) must be(OK)
      contentAsString(result) must include("Accept and complete section")
      contentAsString(result) mustNot include("Confirm and continue")
    }


    "load the summary page with the correct link text when the section is complete" in new Fixture {

      val model = BankDetails(None, None, hasAccepted = true)

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get(true)(request)

      status(result) must be(OK)
      contentAsString(result) must include("Confirm and continue")
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)

      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "show bank account details on the Check your Answers page" in new Fixture {

      val model = BankDetails(
        Some(PersonalAccount),
        Some(BankAccount("Account Name", UKAccount("12341234","000000")))
      )

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

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

        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("bankdetails.summary.nobank.account"))
      }
    }

    "not show edit links" when {
      "on check-your-answers page in amendments" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some(BankAccount("Account Name", UKAccount("12341234","000000")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

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
          Some(BankAccount("Account Name", UKAccount("12341234","000000")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

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
          Some(BankAccount("Account Name", UKAccount("12341234","000000")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

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
          Some(BankAccount("Account Name", UKAccount("12341234","000000")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

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

        val emptyCache = CacheMap("", Map.empty)

        val newRequest = request.withFormUrlEncodedBody("hasAccepted" -> "true")

        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(controller.dataCache.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(emptyCache))

        val result = controller.post()(newRequest)

        status(result) must be(SEE_OTHER)
        redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
      }

      "when there are bank accounts" when {

        "update the accepted flag" in new Fixture {
          val accountType1 = PersonalAccount
          val bankAccount1 = BankAccount("My Account1", UKAccount("111111", "11-11-11"))

          val accountType2 = PersonalAccount
          val bankAccount2 = BankAccount("My Account2", UKAccount("222222", "22-22-22"))

          val accountType3 = PersonalAccount
          val bankAccount3 = BankAccount("My Account3", UKAccount("333333", "33-33-33"))

          val accountType4 = PersonalAccount
          val bankAccount4 = BankAccount("My Account4", UKAccount("444444", "44-44-44"))

          val Model1 = BankDetails(Some(accountType1), Some(bankAccount1))
          val Model2 = BankDetails(Some(accountType2), Some(bankAccount2))
          val Model3 = BankDetails(Some(accountType3), Some(bankAccount3))
          val Model4 = BankDetails(Some(accountType4), Some(bankAccount4))

          val completeModel1 = BankDetails(Some(accountType1), Some(bankAccount1), hasAccepted = true)
          val completeModel2 = BankDetails(Some(accountType2), Some(bankAccount2), hasAccepted = true)
          val completeModel3 = BankDetails(Some(accountType3), Some(bankAccount3), hasAccepted = true)
          val completeModel4 = BankDetails(Some(accountType4), Some(bankAccount4), hasAccepted = true)

          val bankAccounts = Seq(Model1,Model2,Model3,Model4)

          val emptyCache = CacheMap("", Map.empty)

          val newRequest = request.withFormUrlEncodedBody("hasAccepted" -> "true")

          when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(bankAccounts)))

          when(controller.dataCache.save[Seq[BankDetails]](any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(emptyCache))

          val result = controller.post()(newRequest)

          status(result) must be(SEE_OTHER)
          redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))

          verify(controller.dataCache).save[Seq[BankDetails]](any(),
            meq(Seq(completeModel1, completeModel2,completeModel3,completeModel4)))(any(), any(), any())
        }

      }
    }
  }
}
