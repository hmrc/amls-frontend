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

package controllers.declaration

import connectors.DataCacheConnector
import controllers.actions.SuccessfulAuthAction
import models.declaration.AddPerson
import models.declaration.release7.RoleWithinBusinessRelease7
import models.registrationprogress.{Completed, Started, TaskRow}
import models.renewal._
import models.status.{NotCompleted, ReadyForRenewal, SubmissionReadyForReview}
import models.{Country, ReadStatusResponse, SubscriptionFees, SubscriptionResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.{RenewalService, SectionsProvider}
import utils.{AmlsSpec, DependencyMocks}
import views.html.declaration.DeclareView

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future
import scala.language.postfixOps

class DeclarationControllerSpec extends AmlsSpec with MockitoSugar with ScalaFutures {

  trait Fixture extends DependencyMocks {
    self =>
    val request = addToken(authRequest)
    val mockSectionsProvider = mock[SectionsProvider]
    lazy val view = app.injector.instanceOf[DeclareView]
    val renewalService: RenewalService = mock[RenewalService]
    when(renewalService.isRenewalFlow(any(), any(), any())(any(), any())).thenReturn(Future.successful(false))

    val declarationController = new DeclarationController(
      authAction = SuccessfulAuthAction, ds = commonDependencies,
      dataCacheConnector = mock[DataCacheConnector],
      statusService = mockStatusService,
      cc = mockMcc,
      sectionsProvider = mockSectionsProvider,
      renewalService = renewalService,
      view = view
    )
    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "", Some(SubscriptionFees(
        registrationFee = 0,
        fpFee = None,
        fpFeeRate = None,
        approvalCheckFee = None,
        approvalCheckFeeRate = None,
        premiseFee = 0,
        premiseFeeRate = None,
        totalFees = 0,
        paymentReference = "")
      )
    )
    val pendingReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None,
      None, false)
    val notCompletedReadStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None,
      None, false)
    val addPerson = AddPerson("firstName", Some("middleName"), "lastName",
      RoleWithinBusinessRelease7(Set(models.declaration.release7.InternalAccountant)))

    val completeRenewal = Renewal(
      Some(InvolvedInOtherYes("test")),
      Some(BusinessTurnover.First),
      Some(AMLSTurnover.First),
      Some(AMPTurnover.First),
      Some(CustomersOutsideIsUK(true)),
      Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
      Some(PercentageOfCashPaymentOver15000.First),
      Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true, true, Some("other")))))),
      Some(TotalThroughput("01")),
      Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
      Some(TransactionsInLast12Months("1500")),
      Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
      Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
      Some(CETransactionsInLast12Months("123")),
      hasChanged = true
    )

    when(declarationController.renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

    when(declarationController.renewalService.isRenewalComplete(any(), any())(any()))
      .thenReturn(Future.successful(true))
  }

  "Declaration get" must {
    "with completed sections" must {
      val completedSections = Seq(
        TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
        TaskRow("s2", "/bar", true, Completed, TaskRow.completedTag)
      )

      "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(None))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.get()(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
      }

      "load the declaration page for pre-submissions if name and business matching is found" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(Some(addPerson)))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(addPerson.firstName)
        contentAsString(result) must include(addPerson.middleName mkString)
        contentAsString(result) must include(addPerson.lastName)
        contentAsString(result) must include(Messages("submit.registration"))
      }

      "load the declaration page for pre-submissions if name and business matching is found (renewal)" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(Some(addPerson)))

        mockApplicationStatus(ReadyForRenewal(Some(LocalDate.now())))

        val result = declarationController.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(addPerson.firstName)
        contentAsString(result) must include(addPerson.middleName mkString)
        contentAsString(result) must include(addPerson.lastName)
        contentAsString(result) must include(Messages("submit.renewal.application"))
      }

      "load the declaration page for pre-submissions if name and business matching is found (amendment)" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(Some(addPerson)))

        mockApplicationStatus(SubmissionReadyForReview)

        val result = declarationController.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(addPerson.firstName)
        contentAsString(result) must include(addPerson.middleName mkString)
        contentAsString(result) must include(addPerson.lastName)
        contentAsString(result) must include(Messages("submit.amendment.application"))
      }

      "report error if retrieval of amlsRegNo fails" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(Some(addPerson)))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.get()(request)
        status(result) must be(OK)

        contentAsString(result) must include(addPerson.firstName)
        contentAsString(result) must include(addPerson.middleName mkString)
        contentAsString(result) must include(addPerson.lastName)
        contentAsString(result) must include(Messages("submit.registration"))
        contentAsString(result) must include(Messages("declaration.declaration.title"))
      }
    }

    "with incomplete sections" must {
      val incompleteSections = Seq(
        TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
        TaskRow("s2", "/foo", true, Started, TaskRow.incompleteTag)
      )

      "redirect to the RegistrationProgressController" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(incompleteSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(None))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.get()(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }
    }
  }

  "Declaration getWithAmendment" must {
    "with completed sections" must {
      val completedSections = Seq(
        TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
        TaskRow("s2", "/bar", true, Completed, TaskRow.completedTag)
      )

      "load the declaration for amendments page for submissions if name and business matching is found" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(Some(addPerson)))

        val result = declarationController.getWithAmendment()(request)
        status(result) must be(OK)

        contentAsString(result) must include(addPerson.firstName)
        contentAsString(result) must include(addPerson.middleName mkString)
        contentAsString(result) must include(addPerson.lastName)
        contentAsString(result) must include(Messages("submit.amendment.application"))
        contentAsString(result) must include(Messages("declaration.declaration.amendment.title"))
      }

      "redirect to the declaration-persons page if name and/or business matching not found" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(None))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.getWithAmendment()(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) mustBe Some(routes.AddPersonController.get().url)
      }

      "redirect to the declaration-persons for amendments page if name and/or business matching not found and submission is ready for review" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(completedSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(None))

        mockApplicationStatus(SubmissionReadyForReview)

        val result = declarationController.getWithAmendment()(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) mustBe Some(routes.AddPersonController.getWithAmendment().url)
      }
    }

    "with incomplete sections" must {
      val incompleteSections = Seq(
        TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
        TaskRow("s2", "/bar", true, Started, TaskRow.incompleteTag)
      )

      "redirect to the RegistrationProgressController" in new Fixture {
        when {
          mockSectionsProvider.taskRows(any[String])(any(), any())
        }.thenReturn(Future.successful(incompleteSections))

        when(declarationController.dataCacheConnector.fetch[AddPerson](any(), any())
          (any())).thenReturn(Future.successful(None))

        mockApplicationStatus(NotCompleted)

        val result = declarationController.getWithAmendment(request)
        status(result) must be(SEE_OTHER)

        redirectLocation(result) mustBe Some(controllers.routes.RegistrationProgressController.get().url)
      }
    }
  }
}
