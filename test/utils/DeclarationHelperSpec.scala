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

package utils

import models.responsiblepeople._
import models.status._
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.StatusService
import org.mockito.Mockito.when
import org.mockito.Matchers.any
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.DeclarationHelper._
import play.api.test.Helpers._

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


class DeclarationHelperSpec extends PlaySpec with MustMatchers with MockitoSugar {

  implicit val statusService = mock[StatusService]
  implicit val headerCarrier = mock[HeaderCarrier]
  implicit val authContext = mock[AuthContext]

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

      numberOfPartners(Seq(ResponsiblePeople())) mustBe 0

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

  "statusSubtitle" must {
    "return the correct message string given a SubmissionReady status" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(SubmissionReady)

      await(statusSubtitle) mustBe "submit.registration"
    }

    "return the correct message string given a SubmissionReadyForReview status" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(SubmissionReadyForReview)

      await(statusSubtitle) mustBe "submit.amendment.application"
    }

    "return the correct message string given a SubmissionDecisionApproved status" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(SubmissionDecisionApproved)

      await(statusSubtitle) mustBe "submit.amendment.application"
    }

    "return the correct message string given a ReadyForRenewal status" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(ReadyForRenewal(None))

      await(statusSubtitle) mustBe "submit.renewal.application"
    }

    "return the correct message string given a RenewalSubmitted status" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(RenewalSubmitted(None))

      await(statusSubtitle) mustBe "submit.renewal.application"
    }

    "return an exception when any other status is given" in {

      when{
        statusService.getStatus(any(),any(),any())
      } thenReturn Future.successful(SubmissionWithdrawn)

      a[Exception] mustBe thrownBy(await(statusSubtitle))
    }
  }

  val partnerWithName = ResponsiblePeople(
    personName = Some(PersonName("FirstName1", None, "LastName1")),
    positions = Some(Positions(Set(Partner), None)),
    status = None
  )

  val deletedPartner = ResponsiblePeople(
    personName = Some(PersonName("FirstName2", None, "LastName2")),
    positions = Some(Positions(Set(Partner), None)),
    status = Some(StatusConstants.Deleted)
  )

  val nonPartnerWithName = ResponsiblePeople(
    personName = Some(PersonName("FirstName1", None, "LastName1")),
    positions = Some(Positions(Set(Director), None)),
    status = None
  )

}
