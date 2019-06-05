/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class ResponsiblePersonSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues with OneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
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

          outputRp mustEqual (expectedRp)
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

          outputRp mustEqual (expectedRp)
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

          outputRp mustEqual (expectedRp)
        }
      }
    }

    "reset when resetBasedOnApprovalFlags is called" when {

      "phase 2 feature toggle is true" when {

        "fitAndProper is true and approval is true" in {
          val inputRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(true),
              hasAlreadyPaidApprovalCheck = Some(true)),
            hasAccepted = true,
            hasChanged = true)

          inputRp.resetBasedOnApprovalFlags() mustBe(inputRp)

        }

        "fitAndProper is false and approval is true" in {
          val inputRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = Some(true)),
            hasAccepted = true,
            hasChanged = true)

          val expectedRp = ResponsiblePerson(
            approvalFlags = ApprovalFlags(
              hasAlreadyPassedFitAndProper = Some(false),
              hasAlreadyPaidApprovalCheck = None),
            hasAccepted = false,
            hasChanged = true)

          inputRp.resetBasedOnApprovalFlags() mustBe(expectedRp)
        }
      }
    }

    "Successfully validate if the model is complete when phase 2 feature toggle is true" when {

      "json is complete" when {

        "both Fit and proper and approval are both set only" in {
          completeJsonPresentUkResidentFitAndProperPhase2.as[ResponsiblePerson] must be(completeModelUkResident)
        }

        "will fail if at least one of the approval flags is not defined" in {
          val model = completeModelUkResidentPhase2.copy(approvalFlags = ApprovalFlags(hasAlreadyPaidApprovalCheck = None))

          model.isComplete must be(false)
        }
      }

      "json is complete" when {
        "Fit and proper and approval" in {
          completeJsonPresentUkResidentFitAndProperApprovalPhase2.as[ResponsiblePerson] must be(completeModelUkResident)
        }
      }

      "the model is fully complete" in {
        completeModelUkResidentPhase2.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model is fully complete with no previous name added" in {
        completeModelUkResidentNoPreviousNamePhase2.copy(hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
        completeModelUkResidentPhase2.copy(soleProprietorOfAnotherBusiness = None, hasAccepted = true).isComplete must be(true)
      }

      "the model partially complete with vat registration model is empty" in {
        completeModelUkResidentPhase2.copy(vatRegistered = None).isComplete must be(false)
      }

      "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
        completeModelUkResidentPhase2.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
          vatRegistered = Some(VATRegisteredNo)).isComplete must be(false)
      }

      "the model is incomplete" in {
        incompleteModelUkResidentNoDOBPhase2.copy(hasAccepted = true).isComplete must be(false)
      }

      "the model is not complete" in {
        val initial = ResponsiblePerson(Some(DefaultValues.personName))
        initial.isComplete must be(false)
      }
    }
  }
}