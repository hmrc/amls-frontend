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

package utils

import models.businessactivities.{BusinessActivities, UkAccountantsAddress, WhoIsYourAccountant}
import models.responsiblepeople._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class ControllerHelperSpec extends AmlsSpec with ResponsiblePeopleValues{

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("microservice.services.feature-toggle.phase-2-changes" -> true)
    .build()

  def createCompleteResponsiblePersonSeq: Option[Seq[ResponsiblePerson]] = Some(Seq(completeResponsiblePerson))

  def createRPsWithMissingDoB: Option[Seq[ResponsiblePerson]] = {
    val inCompleteResponsiblePerson: ResponsiblePerson = completeResponsiblePerson.copy(dateOfBirth = None)

    Some(
      Seq(
        completeResponsiblePerson,
        inCompleteResponsiblePerson,
        completeResponsiblePerson
      )
    )
  }

  val accountantNameCompleteModel = Some(BusinessActivities(
    whoIsYourAccountant = Some(WhoIsYourAccountant(accountantsName = "Accountant name",
      accountantsTradingName = None,
      address = UkAccountantsAddress("", "", None, None, "")))))

  val accountantNameInCompleteModel = Some(BusinessActivities(
    whoIsYourAccountant = None))

  "ControllerHelper" must {
    "hasIncompleteResponsiblePerson" must {

      "return false" when {
        "responsiblePerson seq is None" in {
            ControllerHelper.hasIncompleteResponsiblePerson(None) mustEqual false
        }

        "all responsiblePerson are complete" in {
          ControllerHelper.hasIncompleteResponsiblePerson(createCompleteResponsiblePersonSeq) mustEqual false
        }
      }

      "return true" when {
        "any responsiblePerson is not complete" in {
          ControllerHelper.hasIncompleteResponsiblePerson(createRPsWithMissingDoB) mustEqual true
        }
      }
    }

    "return accountant name" when {
      "accountant name is called with complete model" in {
        ControllerHelper.accountantName(accountantNameCompleteModel) mustEqual "Accountant name"
      }
    }

    "return empty string" when {
      "accountant name is called with incomplete model" in {
        ControllerHelper.accountantName(accountantNameInCompleteModel) mustEqual ""
      }
    }
  }
}
