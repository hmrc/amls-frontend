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

import generators.ResponsiblePersonGenerator
import models.businessmatching.updateservice.ResponsiblePeopleFitAndProper
import models.responsiblepeople.{ApprovalFlags, ResponsiblePerson}
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import utils.{AmlsSpec, DependencyMocks, StatusConstants}
import org.mockito.Mockito.verify
import org.mockito.Matchers.{any, eq => eqTo}
import ResponsiblePeopleService._

import scala.concurrent.ExecutionContext.Implicits.global

class ResponsiblePersonServiceSpec extends AmlsSpec with ResponsiblePersonGenerator with ScalaFutures {

  trait Fixture extends DependencyMocks {

    // scalastyle:off magic.number
    val responsiblePeople = Gen.listOfN(5, responsiblePersonGen).sample.get

    mockCacheFetch[Seq[ResponsiblePerson]](Some(responsiblePeople), Some(ResponsiblePerson.key))

    val service = new ResponsiblePeopleService(mockCacheConnector)
  }

  "getAll" must {
    "simply return all the people" in new Fixture {
      await(service.getAll) mustBe responsiblePeople
    }
  }

  "getActive" must {
    "return only the people who are not deleted or not complete" in new Fixture {

    }
  }

  "updateResponsiblePeople" must {
    "save fit and proper as true to responsible people to those matched by index" which {
      "will save fit and proper as false to responsible people to those not matched by index" when {

        "a single selection is made" in new Fixture {
          val indices = Set(1)
          val result = service.updateFitAndProperFlag(responsiblePeople, indices)

          result mustBe Seq(
            responsiblePeople.head.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true),
            responsiblePeople(1).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)), hasAccepted = true, hasChanged = true),
            responsiblePeople(2).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true),
            responsiblePeople(3).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true),
            responsiblePeople.last.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true)
          )
        }

        "multiple selections are made" in new Fixture {
          val indices = Set(0, 3, 4)
          val result = service.updateFitAndProperFlag(responsiblePeople, indices)

          result mustBe Seq(
              responsiblePeople.head.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)), hasAccepted = true, hasChanged = true),
              responsiblePeople(1).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true),
              responsiblePeople(2).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(false)), hasAccepted = true, hasChanged = true),
              responsiblePeople(3).copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)), hasAccepted = true, hasChanged = true),
              responsiblePeople.last.copy(approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)), hasAccepted = true, hasChanged = true)
          )
        }
      }
    }
  }

  "A list of responsible people" can {
    "be filtered to only include people not deleted or are incomplete" in new Fixture {

      val people = responsiblePeople.patch(0, Seq(
        responsiblePersonGen.sample.get.copy(status = Some(StatusConstants.Deleted)),
        responsiblePersonGen.sample.get.copy(personName = None)),
        2).zipWithIndex

      val filtered = people.exceptInactive

      filtered.collect { case (p, _) if p.status.contains(StatusConstants.Deleted) => p } mustBe empty
      filtered.collect { case (p, _) if !p.isComplete => p } mustBe empty
    }
  }
}
