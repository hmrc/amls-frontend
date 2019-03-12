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
import models.supervision._
import org.joda.time.LocalDate
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class ControllerHelperSpec extends AmlsSpec with ResponsiblePeopleValues with DependencyMocks {

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

    "anotherBodyComplete" must {
      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for AnotherBodyNo " in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyNo))), Supervision.key)

        ControllerHelper.anotherBodyComplete(mockCacheMap) mustBe Some(true, false)
      }

      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for AnotherBodyYes " in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes(supervisorName = "Name")))), Supervision.key)

        ControllerHelper.anotherBodyComplete(mockCacheMap) mustBe Some(false, true)
      }

      "return tuple of (is anotherBodyComplete, is anotherBodyYes) for complete AnotherBodyYes " in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes("Name",
          Some(SupervisionStart(new LocalDate(1990, 2, 24))),
          Some(SupervisionEnd(new LocalDate(1998, 2, 24))),
          Some(SupervisionEndReasons("Reason")))))), Supervision.key)

        ControllerHelper.anotherBodyComplete(mockCacheMap) mustBe Some(true, true)
      }
    }

    "isAnotherBodyYes" must {
      "return true if AnotherBody is instance of AnotherBodyYes" in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes(supervisorName = "Name")))), Supervision.key)

        ControllerHelper.isAnotherBodyYes(ControllerHelper.anotherBodyComplete(mockCacheMap)) mustBe true
      }

      "return false if AnotherBody is AnotherBodyNo" in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyNo))), Supervision.key)

        ControllerHelper.isAnotherBodyYes(ControllerHelper.anotherBodyComplete(mockCacheMap)) mustBe false
      }
    }

    "isAnotherBodyComplete" must {
      "return true if AnotherBodyYes is complete" in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes("Name",
          Some(SupervisionStart(new LocalDate(1990, 2, 24))),
          Some(SupervisionEnd(new LocalDate(1998, 2, 24))),
          Some(SupervisionEndReasons("Reason")))))), Supervision.key)

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(mockCacheMap)) mustBe true
      }

      "return false if AnotherBodyYes is incomplete" in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyYes("Name",
          Some(SupervisionStart(new LocalDate(1990, 2, 24))),
          Some(SupervisionEnd(new LocalDate(1998, 2, 24))))))), Supervision.key)

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(mockCacheMap)) mustBe false
      }

      "return true if AnotherBody is AnotherBodyNo" in {
        mockCacheGetEntry[Supervision](Some(Supervision(Some(AnotherBodyNo))), Supervision.key)

        ControllerHelper.isAnotherBodyComplete(ControllerHelper.anotherBodyComplete(mockCacheMap)) mustBe true
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
