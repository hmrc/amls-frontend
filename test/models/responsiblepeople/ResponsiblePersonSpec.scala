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

package models.responsiblepeople

import org.mockito.ArgumentMatchers.{eq => meq, _}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import services.cache.Cache

class ResponsiblePersonSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      "play.filters.disabled" -> List("uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter")
    )
    .build()

  "ResponsiblePeople" must {
    "calling updateFitAndProperAndApproval" must {

      val inputRp = ResponsiblePerson()

      "set Approval flag to None" when {

        "only if both choice and msbOrTcsp are false so the answer to the approval question is needed" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = false, msbOrTcsp = false)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = None
            ),
            hasChanged = true
          )

          outputRp mustEqual expectedRp
        }
      }

      "set Approval to match the incoming fitAndProper flag" when {

        "choice is true, and msbOrTcsp is false so the answer to the approval question in not required" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = true, msbOrTcsp = false)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(true),
              hasAlreadyPaidApprovalCheck = Some(true)
            ),
            hasChanged = true
          )

          outputRp mustEqual expectedRp
        }

        "choice is false, and msbOrTcsp is true so the answer to the approval question is not needed" in {

          val outputRp = inputRp.updateFitAndProperAndApproval(fitAndPropperChoice = false, msbOrTcsp = true)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(false)
            ),
            hasChanged = true
          )

          outputRp mustEqual expectedRp
        }
      }
    }

    "reset when resetBasedOnApprovalFlags is called" when {
      "fitAndProper is true and approval is true" in {
        val inputRp = ResponsiblePerson(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true
        )

        inputRp.resetBasedOnApprovalFlags() mustBe inputRp
      }

      "fitAndProper is false and approval is true" in {
        val inputRp = ResponsiblePerson(
          approvalFlags =
            ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = Some(true)),
          hasAccepted = true,
          hasChanged = true
        )

        val expectedRp = ResponsiblePerson(
          approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false), hasAlreadyPaidApprovalCheck = None),
          hasAccepted = false,
          hasChanged = true
        )

        inputRp.resetBasedOnApprovalFlags() mustBe expectedRp
      }
    }

    "set hasPreviousName correctly" when {

      "given true" in {
        val updated = ResponsiblePerson(legalName = Some(PreviousName(None, None, None, None))).hasPreviousName(true)

        updated.legalName.flatMap(_.hasPreviousName).value mustBe true
      }

      "given false" in {
        val updated = ResponsiblePerson(
          legalName = Some(PreviousName(Some(true), Some("first"), Some("middle"), Some("last")))
        ).hasPreviousName(false)

        updated.legalName.flatMap(_.hasPreviousName).value mustBe false
      }
    }

    "Successfully validate if the model is complete" when {
      "json is complete" when {
        "both Fit and proper and approval are both set only" in {
          completeJsonPresentUkResidentFitAndProper.as[ResponsiblePerson] must be(completeModelUkResidentFPtrue)
        }

        "will fail if at least one of the approval flags is not defined" in {
          val model = completeModelUkResident.copy(approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None))

          model.isComplete must be(false)
        }
      }

      "json is complete" when {
        "Fit and proper and approval" in {
          completeJsonPresentUkResidentFitAndProperApproval.as[ResponsiblePerson] must be(completeModelUkResidentFPtrue)
        }
      }

      "the model is fully complete" in {
        completeModelUkResident.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model is fully complete with no previous name added" in {
        completeModelUkResidentNoPreviousName.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
        completeModelUkResident.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(
          true
        )
      }

      "the model partially complete with vat registration model is empty" in {
        completeModelUkResident.copy(vatRegistered = None).isComplete must be(false)
      }

      "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
        completeModelUkResident
          .copy(
            soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
            vatRegistered = Some(VATRegisteredNo)
          )
          .isComplete must be(false)
      }

      "the model is incomplete" in {
        incompleteModelUkResidentNoDOB.copy(hasAccepted = true).isComplete must be(false)
      }

      "the model is not complete" in {
        val initial = ResponsiblePerson(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }
    }

    "have taskRow function which" when {

      val messages = Helpers.stubMessages()

      "called" must {
        "return NotStarted task row if model is empty" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(ResponsiblePerson())))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.NotStarted)
        }

        "return Started task row if model is not empty" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(incompleteResponsiblePeople)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
        }

        "return Completed task row if model is not empty and has complete rp" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Completed)
        }

        "return Completed task row if model is not empty and has complete rp that has changed" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson.copy(hasChanged = true))))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Updated)
        }

        "return Started task row if model is not empty and has complete and incomplete rps" in {
          val mockCacheMap = mock[Cache]

          when(mockCacheMap.getEntry[Seq[ResponsiblePerson]](meq(ResponsiblePerson.key))(any()))
            .thenReturn(Some(Seq(completeResponsiblePerson, incompleteResponsiblePeople)))

          ResponsiblePerson.taskRow(mockCacheMap, messages).status must be(models.registrationprogress.Started)
        }
      }
    }
  }
}
