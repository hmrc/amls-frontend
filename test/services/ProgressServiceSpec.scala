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

package services

import connectors.{AmlsConnector, DataCacheConnector}
import models.ReadStatusResponse
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, NotStarted, Section}
import models.responsiblepeople.{PersonName, _}
import models.status._
import org.joda.time.{DateTimeUtils, LocalDate, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class ProgressServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures with OneAppPerSuite {

  object TestProgressService extends ProgressService {

    override private[services] val cacheConnector: DataCacheConnector = mock[DataCacheConnector]
    override private[services] val statusService: StatusService = mock [StatusService]
  }

  implicit val hc = mock[HeaderCarrier]
  implicit val ac = mock[AuthContext]
  implicit val ec = mock[ExecutionContext]


  "Progress Service" must {
    "return fee guidance url" when {
      "business is a partnership and there are 2 partners and 1 nominated officer" in {

        val positions = Positions(Set(BeneficialOwner, Partner, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.Partnership),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.routes.FeeGuidanceController.get())
        }

      }

      "business is not a prtnership and at least one of the person in responsible people is the nominated officer" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.routes.FeeGuidanceController.get())
        }
      }
    }

    "return register partners url" when {
      "business is a partnership and there are less than 2 partners" in {
        val positions = Positions(Set(BeneficialOwner, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.Partnership),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.RegisterPartnersController.get())
        }
      }
    }

    "return who is registering url" when {
      "status is amendment and there is a nominated officer" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
        }
      }

      "status is variation and there is a nominated officer" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.getWithAmendment())
        }
      }

      "status is renewal and there is a nominated officer" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(ReadyForRenewal(None)))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.getWithRenewal())
        }
      }
    }

    "return Who is the business’s nominated officer? url" when {
      "there is no selected nominated officer" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any())).
          thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
        }
      }

      "there is no selected nominated officer and status is amendment" in {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePeople(Some(PersonName("first1", Some("middle"), "last1", None, None)), None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePeople(Some(PersonName("first2", None, "last2", None, None)), None, None, None, None, None, None, Some(positions))
        val responsiblePeople = Seq(rp1, rp2)
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any())).
          thenReturn(Future.successful(Some(responsiblePeople)))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
        }

      }


    }

    "respond with NOT_FOUND" when {
      "there are no responsible people" in {
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        when(TestProgressService.cacheConnector.fetch[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key))(any(), any(), any()))
          .thenReturn(Future.successful(None))

        when(TestProgressService.statusService.getStatus(any(), any(), any()))
          .thenReturn(Future.successful(SubmissionReady))

        when(TestProgressService.cacheConnector.fetch[BusinessMatching](eqTo(BusinessMatching.key))(any(), any(), any()))
          .thenReturn(Future.successful(Some(businessMatching)))

        whenReady(TestProgressService.getSubmitRedirect) {
          _ mustEqual None
        }
      }
    }

  }
}
