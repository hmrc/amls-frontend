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

package controllers.bankdetails

import controllers.actions.SuccessfulAuthAction
import models.bankdetails.BankAccountType._
import models.bankdetails._
import models.status.SubmissionReady
import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.Helpers._
import play.api.test.Injecting
import utils.bankdetails.CheckYourAnswersHelper
import utils.{AmlsSpec, DependencyMocks}
import views.html.bankdetails.CheckYourAnswersView

import scala.concurrent.Future

class SummaryControllerSpec extends AmlsSpec with MockitoSugar with Injecting {

  trait Fixture extends DependencyMocks {
    self =>
    val request: Request[AnyContentAsEmpty.type] = addToken(authRequest)

    val ukAccount: BankAccount                 = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("123456789", "111111")))
    val nonUkIban: BankAccount                 = BankAccount(
      Some(BankAccountIsUk(false)),
      Some(BankAccountHasIban(true)),
      Some(NonUKIBANNumber("DE89370400440532013000"))
    )
    val nonUkAccount: BankAccount              = BankAccount(
      Some(BankAccountIsUk(false)),
      Some(BankAccountHasIban(false)),
      Some(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"))
    )
    lazy val summaryView: CheckYourAnswersView = inject[CheckYourAnswersView]

    val controller = new SummaryController(
      dataCacheConnector = mockCacheConnector,
      authAction = SuccessfulAuthAction,
      ds = commonDependencies,
      mcc = mockMcc,
      inject[CheckYourAnswersHelper],
      view = summaryView
    )
  }

  "Get" must {

    "load the summary page with the correct text when UK" in new Fixture {

      val model1: BankDetails = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount))
      val model2: BankDetails = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))
      mockApplicationStatus(SubmissionReady)

      val result: Future[Result] = controller.get(1)(request)

      status(result)          must be(OK)
      contentAsString(result) must include("My Personal Account")
      contentAsString(result) must include("11-11-11")
      contentAsString(result) must include("123456789")
      contentAsString(result) must include(messages("bankdetails.bankaccount.sortcode"))
      contentAsString(result) mustNot include("My IBAN Account")
    }

    "redirect to RegistrationProgressController when BankDetails cannot be retrieved" in new Fixture {

      mockCacheFetch[Seq[BankDetails]](None)
      mockApplicationStatus(SubmissionReady)

      val result: Future[Result] = controller.get(1)(request)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get().url))
    }

    "load the summary page with correct text when IBAN" in new Fixture {

      val model1: BankDetails = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount))
      val model2: BankDetails = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))
      mockApplicationStatus(SubmissionReady)

      val result: Future[Result] = controller.get(2)(request)

      status(result)          must be(OK)
      contentAsString(result) must include("My IBAN Account")
      contentAsString(result) must include("DE89370400440532013000")
      contentAsString(result) must include(messages("bankdetails.bankaccount.iban.title"))
      contentAsString(result) mustNot include("My Personal Account")
    }
  }

  "post is called" must {
    "respond with OK and redirect to the bank account details page" in new Fixture {

      val model1: BankDetails = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount))
      val model2: BankDetails = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban))

      mockCacheFetch[Seq[BankDetails]](Some(Seq(model1, model2)))

      mockCacheSave[Seq[BankDetails]]

      val result: Future[Result] = controller.post(2)(request)

      status(result)           must be(SEE_OTHER)
      redirectLocation(result) must be(Some(controllers.bankdetails.routes.YourBankAccountsController.get().url))
    }

    "update the accepted flag" in new Fixture {
      val model1: BankDetails =
        BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount), hasAccepted = true)
      val model2: BankDetails = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban))

      val completeModel1: BankDetails =
        BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount), hasAccepted = true)
      val completeModel2: BankDetails =
        BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban), hasAccepted = true)

      val bankAccounts: Seq[BankDetails] = Seq(model1, model2)

      mockCacheFetch[Seq[BankDetails]](Some(bankAccounts))
      mockCacheSave[Seq[BankDetails]]

      val result: Future[Result] = controller.post(2)(request)

      status(result) must be(SEE_OTHER)

      verify(controller.dataCacheConnector)
        .save[Seq[BankDetails]](any(), any(), meq(Seq(completeModel1, completeModel2)))(any())
    }
    "remove the itemIndex from session if there was one present" in new Fixture {
      override val request: Request[AnyContentAsEmpty.type] = addTokenWithSessionParam(authRequest)("itemIndex" -> "4")

      val model1: BankDetails =
        BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukAccount), hasAccepted = true)
      val model2: BankDetails = BankDetails(Some(BelongsToBusiness), Some("My IBAN Account"), Some(nonUkIban))

      val bankAccounts: Seq[BankDetails] = Seq(model1, model2)

      mockCacheFetch[Seq[BankDetails]](Some(bankAccounts))
      mockCacheSave[Seq[BankDetails]]
      val result: Future[Result] = controller.post(2)(request)

      status(result) must be(SEE_OTHER)
      session(result).get("itemIndex") mustBe None
    }
  }
}
