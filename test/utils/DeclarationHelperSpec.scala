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

package utils

import models.Country
import models.registrationprogress.{Completed, Started, TaskRow, Updated}
import models.renewal._
import models.responsiblepeople._
import models.status._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Messages
import play.api.test.Helpers
import play.api.test.Helpers._
import services.{RenewalService, SectionsProvider, StatusService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.DeclarationHelper._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class DeclarationHelperSpec extends PlaySpec with Matchers with MockitoSugar {

  implicit val statusService: StatusService = mock[StatusService]
  implicit val renewalService: RenewalService = mock[RenewalService]
  implicit val headerCarrier: HeaderCarrier = mock[HeaderCarrier]

  val amlsRegNo: Option[String] = Some("regNo")
  val accountTypeId: (String, String) = ("accountType", "accountId")
  val credId = "12341234"

  private val completeRenewal = Renewal(
    Some(InvolvedInOtherYes("test")),
    Some(BusinessTurnover.First),
    Some(AMLSTurnover.First),
    Some(AMPTurnover.First),
    Some(CustomersOutsideIsUK(true)),
    Some(CustomersOutsideUK(Some(Seq(Country("United Kingdom", "GB"))))),
    Some(PercentageOfCashPaymentOver15000.First),
    Some(CashPayments(CashPaymentsCustomerNotMet(true), Some(HowCashPaymentsReceived(PaymentMethods(true,true,Some("other")))))),
    Some(TotalThroughput("01")),
    Some(WhichCurrencies(Seq("EUR"), None, Some(MoneySources(None, None, None)))),
    Some(TransactionsInLast12Months("1500")),
    Some(SendTheLargestAmountsOfMoney(Seq(Country("United Kingdom", "GB")))),
    Some(MostTransactions(Seq(Country("United Kingdom", "GB")))),
    Some(CETransactionsInLast12Months("123")),
    hasChanged = true
  )

  private val inCompleteRenewal = completeRenewal.copy(
    businessTurnover = None
  )

  implicit val messages: Messages = Helpers.stubMessages()

  "currentPartnersNames" must {
    "return a sequence of full names of only undeleted partners" in {

      currentPartnersNames(Seq(
        partnerWithName
      )) mustBe Seq("FirstName1 LastName1")

      currentPartnersNames(Seq(
        partnerWithName,
        deletedPartner,
        nonPartnerWithName
      )) mustBe Seq("FirstName1 LastName1")
    }
    "return an empty sequence when there are no partners" in {
      currentPartnersNames(Seq(
        nonPartnerWithName
      )) mustBe Seq.empty
    }
  }

  "numberOfPartners" must {
    "return 0 when there are no partners" in {

      numberOfPartners(Seq(ResponsiblePerson())) mustBe 0

    }

    "return the correct number when there are one or more partners" in {

      numberOfPartners(Seq(
        partnerWithName
      )) mustBe 1

      numberOfPartners(Seq(
        partnerWithName,
        partnerWithName
      )) mustBe 2
    }

    "not count responsible people whose status is Deleted" in {

      numberOfPartners(Seq(deletedPartner)) mustBe 0

      numberOfPartners(Seq(
        deletedPartner,
        deletedPartner,
        partnerWithName
      )) mustBe 1
    }
  }

  "nonPartners" must {
    "return a sequence of responsiblePeople containing no partners" in {
      nonPartners(Seq(
        partnerWithName,
        deletedPartner,
        nonPartnerWithName
      )) mustBe Seq(nonPartnerWithName)
    }

    "return an empty sequence when there are no non-partners" in {
      nonPartners(Seq(
        partnerWithName,
        deletedPartner
      )) mustBe Seq.empty
    }
  }

  "statusSubtitle(amlsRegNo, accountTypeId, credId" must {
    "return the correct message string given a SubmissionReady status" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionReady)

      await(statusSubtitle(amlsRegNo, accountTypeId, credId)) mustBe "submit.registration"
    }

    "return the correct message string given a SubmissionReadyForReview status" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionReadyForReview)

      await(statusSubtitle(amlsRegNo, accountTypeId, credId)) mustBe "submit.amendment.application"
    }

    "return the correct message string given a SubmissionDecisionApproved status" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionDecisionApproved)

      await(statusSubtitle(amlsRegNo, accountTypeId, credId)) mustBe "submit.amendment.application"
    }

    "return the correct message string given a ReadyForRenewal status" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(ReadyForRenewal(None))

      await(statusSubtitle(amlsRegNo, accountTypeId, credId)) mustBe "submit.renewal.application"
    }

    "return the correct message string given a RenewalSubmitted status" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(RenewalSubmitted(None))

      await(statusSubtitle(amlsRegNo, accountTypeId, credId)) mustBe "submit.renewal.application"
    }

    "return an exception when any other status is given" in {

      when{
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionWithdrawn)

      a[Exception] mustBe thrownBy(await(statusSubtitle(amlsRegNo, accountTypeId, credId)))
    }
  }

  "promptRenewal" must {
    "where in renewal window" must {
      "where renewal incomplete" must {
        "return true" in {
          when {
            statusService.getStatus(any(),any(), any())(any(),any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now())))

          when(renewalService.isRenewalComplete(any(), any())(any()))
            .thenReturn(Future.successful(false))

          when(renewalService.getRenewal(any())).thenReturn(Future.successful(Some(inCompleteRenewal)))

          await(promptRenewal(amlsRegNo, accountTypeId, credId)) mustBe(true)
        }
      }

      "where no renewal" must {
        "return true" in {
          when {
            statusService.getStatus(any(),any(), any())(any(),any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now())))

          when(renewalService.isRenewalComplete(any(), any())(any()))
            .thenReturn(Future.successful(false))

          when(renewalService.getRenewal(any())).thenReturn(Future.successful(None))

          await(promptRenewal(amlsRegNo, accountTypeId, credId)) mustBe(true)
        }
      }

      "where renewal complete" must {
        "return false" in {
          when {
            statusService.getStatus(any(),any(), any())(any(),any(), any())
          } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now())))

          when(renewalService.isRenewalComplete(any(), any())(any()))
            .thenReturn(Future.successful(true))

          when(renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

          await(promptRenewal(amlsRegNo, accountTypeId, credId)) mustBe(false)
        }
      }
    }

    "where not in renewal window post renewal" must {
      "return false" in {
        when{
          statusService.getStatus(any(),any(), any())(any(),any(), any())
        } thenReturn Future.successful(RenewalSubmitted(None))

        when(renewalService.isRenewalComplete(any(), any())(any()))
          .thenReturn(Future.successful(true))

        when(renewalService.getRenewal(any())).thenReturn(Future.successful(Some(completeRenewal)))

        await(promptRenewal(amlsRegNo, accountTypeId, credId)) mustBe(false)
      }
    }

    "where not in renewal window pre renewal" must {
      "return false" in {
        when{
          statusService.getStatus(any(),any(),any())(any(),any(), any())
        } thenReturn Future.successful(SubmissionReady)

        when(renewalService.getRenewal(any())).thenReturn(Future.successful(None))

        await(promptRenewal(amlsRegNo, accountTypeId, credId)) mustBe(false)
      }
    }
  }

  "statusEndDate" must {
    "return the end date where there is a date" in {
      val date = LocalDate.now()
      when {
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(ReadyForRenewal(Some(date)))

      await(statusEndDate(amlsRegNo, accountTypeId, credId)) mustBe(Some(date))
    }

    "return none where there is no date" in {
      when{
        statusService.getStatus(any(),any(),any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionReady)

      await(statusEndDate(amlsRegNo, accountTypeId, credId)) mustBe(None)
    }
  }

  "getSubheadingBasedOnStatus" must {
    "return renewal subheading if application is in renewal window and renewal is complete" in {
      when {
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(ReadyForRenewal(Some(LocalDate.now())))

      when(renewalService.isRenewalComplete(any(), any())(any()))
        .thenReturn(Future.successful(true))

      val result = getSubheadingBasedOnStatus(credId, amlsRegNo, accountTypeId, statusService, renewalService).value
      await(result) mustBe Some("submit.amendment.application")
    }

    "return submit application subheading if application is ready to submit" in {
      when {
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionReady)

      val result = getSubheadingBasedOnStatus(credId, amlsRegNo, accountTypeId, statusService, renewalService).value
      await(result) mustBe Some("submit.registration")
    }

    "return update information subheading if application is in any other state" in {
      when {
        statusService.getStatus(any(),any(), any())(any(),any(), any())
      } thenReturn Future.successful(SubmissionReadyForReview)

      val result = getSubheadingBasedOnStatus(credId, amlsRegNo, accountTypeId, statusService, renewalService).value
      await(result) mustBe Some("submit.amendment.application")
    }
  }

  "sectionsComplete" must {
    val sectionsProvider = mock[SectionsProvider]
    val completedSections = Seq(
      TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
      TaskRow("s2", "/bar", true, Completed, TaskRow.completedTag)
    )

    val incompleteSections = Seq(
      TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
      TaskRow("s2", "/bar", true, Started, TaskRow.incompleteTag)
    )

    val completedAndUpdatedSections = Seq(
      TaskRow("s1", "/foo", true, Completed, TaskRow.completedTag),
      TaskRow("s2", "/bar", true, Updated, TaskRow.updatedTag)
    )

    "return false where one or more sections are incomplete" in {
      when{
        sectionsProvider.taskRows(eqTo(credId))(any(), any())
      }.thenReturn(Future.successful(incompleteSections))

      await(sectionsComplete(credId, sectionsProvider, false)) mustBe(false)
    }

    "return true where all sections are complete" in {
      when{
        sectionsProvider.taskRows(eqTo(credId))(any(), any())
      }.thenReturn(Future.successful(completedSections))

      await(sectionsComplete(credId, sectionsProvider, false)) mustBe(true)
    }

    "return true where sections are either complete or updated" in {

      when(sectionsProvider.taskRows(eqTo(credId))(any(), any()))
        .thenReturn(Future.successful(completedAndUpdatedSections))

      await(sectionsComplete(credId, sectionsProvider, false)) mustBe true
    }
  }

  val partnerWithName: ResponsiblePerson = ResponsiblePerson(
    personName = Some(PersonName("FirstName1", None, "LastName1")),
    positions = Some(Positions(Set(Partner), None)),
    status = None
  )

  val deletedPartner: ResponsiblePerson = ResponsiblePerson(
    personName = Some(PersonName("FirstName2", None, "LastName2")),
    positions = Some(Positions(Set(Partner), None)),
    status = Some(StatusConstants.Deleted)
  )

  val nonPartnerWithName: ResponsiblePerson = ResponsiblePerson(
    personName = Some(PersonName("FirstName1", None, "LastName1")),
    positions = Some(Positions(Set(Director), None)),
    status = None
  )

}
