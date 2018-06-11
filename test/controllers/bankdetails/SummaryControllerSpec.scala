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
    self =>
    val request = addToken(authRequest)

    val controller = new SummaryController(
      dataCacheConnector = mockCacheConnector,
      authConnector = self.authConnector
    )
  }

  "Get" must {

    "load the summary page with the correct text when UK" in new Fixture {

      val model1 = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))
      val model2 = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(NonUKIBANNumber("DE89370400440532013000")))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))
      mockApplicationStatus(SubmissionReady)

      val result = controller.get(1)(request)

      status(result) must be(OK)
      contentAsString(result) must include("My Personal Account")
      contentAsString(result) must include("11-11-11")
      contentAsString(result) must include("123456789")
      contentAsString(result) must include(Messages("bankdetails.bankaccount.sortcode"))
      contentAsString(result) mustNot include("My IBAN Account")
    }


    "load the summary page with correct text when IBAN" in new Fixture {

      val model1 = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))
      val model2 = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(NonUKIBANNumber("DE89370400440532013000")))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))
      mockApplicationStatus(SubmissionReady)

      val result = controller.get(2)(request)

      status(result) must be(OK)
      contentAsString(result) must include("My IBAN Account")
      contentAsString(result) must include("DE89370400440532013000")
      contentAsString(result) must include(Messages("bankdetails.bankaccount.iban"))
      contentAsString(result) mustNot include("My Personal Account")
    }
  }

  "post is called" must {
    "respond with OK and redirect to the bank account details page" in new Fixture {

      val model1 = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")))
      val model2 = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(NonUKIBANNumber("DE89370400440532013000")))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))

      mockCacheSave[Seq[BankDetails]]

      val result = controller.post(2)(request)

      status(result) must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.bankdetails.routes.YourBankAccountsController.get().url))
    }

    "update the accepted flag" in new Fixture {
      val model1 = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")), hasAccepted = true)
      val model2 = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(NonUKIBANNumber("DE89370400440532013000")))

      val completeModel1 = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(UKAccount("123456789", "111111")), hasAccepted = true)
      val completeModel2 = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(NonUKIBANNumber("DE89370400440532013000")), hasAccepted = true)

      val bankAccounts = Seq(model1, model2)

      mockCacheFetch[Seq[BankDetails]](Some(bankAccounts))
      mockCacheSave[Seq[BankDetails]]

      val result = controller.post(2)(request)

      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector).save[Seq[BankDetails]](any(),
        meq(Seq(completeModel1, completeModel2)))(any(), any(), any())
    }
  }
}
