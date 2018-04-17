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
import models.responsiblepeople.ResponsiblePeople
import org.scalacheck.Gen
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import utils.{DependencyMocks, StatusConstants}

import scala.concurrent.ExecutionContext.Implicits.global

class ResponsiblePeopleServiceSpec extends PlaySpec with ResponsiblePersonGenerator with ScalaFutures {

  trait Fixture extends DependencyMocks {

    // scalastyle:off magic.number
    val people = Gen.listOfN(10, responsiblePersonGen).sample.get

    mockCacheFetch[Seq[ResponsiblePeople]](Some(people), Some(ResponsiblePeople.key))

    val service = new ResponsiblePeopleService(mockCacheConnector)
  }

  "getAll" must {
    "simply return all the people" in new Fixture {
      await(service.getAll) mustBe people
    }
  }

  "getActive" must {
    "return only the people who are not deleted or not complete" in new Fixture {
      val p = people.patch(0, Seq(
        responsiblePersonGen.sample.get.copy(status = Some(StatusConstants.Deleted)),
        responsiblePersonGen.sample.get.copy(personName = None)),
        2)

      val filtered = p filter people.contains

      mockCacheFetch[Seq[ResponsiblePeople]](Some(p), Some(ResponsiblePeople.key))

      await(service.getActive) mustBe filtered
    }
  }

  "updateResponsiblePeople" must {
    "save fit and proper as true to responsible people to those matched by index" which {
      "will save fit and proper as false to responsible people to those not matched by index" when {
        "a single selection is made" in new Fixture {

          //          val result = controller.post()(request.withFormUrlEncodedBody("responsiblePeople[]" -> "1"))
          //
          //          status(result) must be(SEE_OTHER)
          //
          //          verify(
          //            controller.dataCacheConnector
          //          ).save[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key), eqTo(Seq(
          //            responsiblePeople.head,
          //            responsiblePeople(1).copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
          //            responsiblePeople(2).copy(hasAlreadyPassedFitAndProper = Some(false), hasAccepted = true, hasChanged = true),
          //            responsiblePeople(3),
          //            responsiblePeople.last
          //          )))(any(), any(), any())

        }
        "multiple selections are made" in new Fixture {

          //          val result = controller.post()(request.withFormUrlEncodedBody(
          //            "responsiblePeople[]" -> "0",
          //            "responsiblePeople[]" -> "3",
          //            "responsiblePeople[]" -> "4"
          //          ))
          //
          //          status(result) must be(SEE_OTHER)
          //
          //          verify(
          //            controller.dataCacheConnector
          //          ).save[Seq[ResponsiblePeople]](eqTo(ResponsiblePeople.key), eqTo(Seq(
          //            responsiblePeople.head.copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
          //            responsiblePeople(1),
          //            responsiblePeople(2).copy(hasAlreadyPassedFitAndProper = Some(false), hasAccepted = true, hasChanged = true),
          //            responsiblePeople(3).copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true),
          //            responsiblePeople.last.copy(hasAlreadyPassedFitAndProper = Some(true), hasAccepted = true, hasChanged = true)
          //          )))(any(), any(), any())

        }
      }
    }
  }
}
