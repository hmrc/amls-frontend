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

package services

import connectors.{AmlsConnector, DataCacheConnector, PayApiConnector}
import controllers.FeeGuidanceController
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
import play.api.inject._
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Call
import play.api.test.Helpers._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.{AuthorisedFixture, DependencyMocks, AmlsSpec}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

class ProgressServiceSpec extends AmlsSpec with MockitoSugar with ScalaFutures with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture with DependencyMocks { self =>

    lazy val defaultBuilder = new GuiceApplicationBuilder()
      .configure("microservice.services.feature-toggle.show-fees" -> true)
      .disable[com.kenshoo.play.metrics.PlayModule]
      .overrides(bind[AuthConnector].to(self.authConnector))
      .overrides(bind[DataCacheConnector].to(mockCacheConnector))
      .overrides(bind[StatusService].to(mockStatusService))

    val builder = defaultBuilder
    lazy val app = builder.build()
    lazy val service = app.injector.instanceOf[ProgressService]

  }

  "Progress Service" must {
    "return fee guidance url" when {
      "business is a partnership and there are 2 partners and 1 nominated officer" in new Fixture {

        val positions = Positions(Set(BeneficialOwner, Partner, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None,None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionReady)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.routes.FeeGuidanceController.get())
        }

      }

      "business is not a prtnership and at least one of the person in responsible people is the nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionReady)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.routes.FeeGuidanceController.get())
        }
      }
    }

    "return register partners url" when {
      "business is a partnership and there are less than 2 partners" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionReady)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.RegisterPartnersController.get())
        }
      }
    }

    "return who is registering url" when {
      "status is amendment and there is a nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionReadyForReview)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
        }
      }

      "status is variation and there is a nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionDecisionApproved)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
        }
      }

      "status is renewal and there is a nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(ReadyForRenewal(None))

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
        }
      }

      "show fees is false" when {
        "business is a partnership and there are 2 partners and 1 nominated officer" in new Fixture {

          override val builder = defaultBuilder.configure("microservice.services.feature-toggle.show-fees" -> false)

          val positions = Positions(Set(BeneficialOwner, Partner, NominatedOfficer), Some(new LocalDate()))
          val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
          val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

          mockApplicationStatus(SubmissionReady)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
          mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

          whenReady(service.getSubmitRedirect) {
            _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
          }

        }

        "business is not a partnership and at least one of the person in responsible people is the nominated officer" in new Fixture {

          override val builder = defaultBuilder.configure("microservice.services.feature-toggle.show-fees" -> false)

          val positions = Positions(Set(BeneficialOwner, InternalAccountant, NominatedOfficer), Some(new LocalDate()))
          val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
          val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

          mockApplicationStatus(SubmissionReady)

          mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
          mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

          whenReady(service.getSubmitRedirect) {
            _ mustEqual Some(controllers.declaration.routes.WhoIsRegisteringController.get())
          }
        }
      }

    }

    "return Who is the business’s nominated officer? url" when {
      "there is no selected nominated officer" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionReady)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.get())
        }
      }

      "there is no selected nominated officer and status is amendment" in new Fixture {
        val positions = Positions(Set(BeneficialOwner, InternalAccountant), Some(new LocalDate()))
        val rp1 = ResponsiblePerson(Some(PersonName("first1", Some("middle"), "last1")), None, None, None, None, None, None, None, None, None, Some(positions))
        val rp2 = ResponsiblePerson(Some(PersonName("first2", None, "last2")), None, None, None, None, None, None, None, None, None, Some(positions))
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

        mockApplicationStatus(SubmissionDecisionApproved)

        mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual Some(controllers.declaration.routes.WhoIsTheBusinessNominatedOfficerController.getWithAmendment())
        }

      }

    }

    "respond with NOT_FOUND" when {
      "there are no responsible people" in new Fixture {
        val businessMatching = BusinessMatching(reviewDetails = Some(
          ReviewDetails(
            "Business Name",
            Some(models.businessmatching.BusinessType.SoleProprietor),
            mock[Address],
            "safeId",
            None
          )
        ))

        mockApplicationStatus(SubmissionReady)

        mockCacheFetch[Seq[ResponsiblePerson]](None, Some(ResponsiblePerson.key))
        mockCacheFetch[BusinessMatching](Some(businessMatching), Some(BusinessMatching.key))

        whenReady(service.getSubmitRedirect) {
          _ mustEqual None
        }
      }
    }

  }
}
