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

package services.responsiblepeople

import connectors.DataCacheConnector
import models.responsiblepeople.{PersonName, ResponsiblePeopleValues, ResponsiblePerson}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import utils.{AmlsSpec, StatusConstants}

import scala.concurrent.Future

class YourResponsiblePeopleServiceSpec extends AmlsSpec with ResponsiblePeopleValues {

  val mockCacheConnector: DataCacheConnector = mock[DataCacheConnector]
  val service                                = new YourResponsiblePeopleService(mockCacheConnector)

  val credId = "1234567890"

  "YourResponsiblePeopleService" when {

    ".completeAndIncompleteRP is called" must {

      "return None" when {

        "fetch returns None" in {

          when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(None)

          service.completeAndIncompleteRP(credId).futureValue mustBe None
        }
      }

      "filter Responsible People" when {

        "status is deleted" in {

          when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(
            Some(
              Seq(
                ResponsiblePerson(status = Some(StatusConstants.Deleted))
              )
            )
          )

          service.completeAndIncompleteRP(credId).futureValue.map(_._2) mustBe Some(Seq.empty[(ResponsiblePerson, Int)])
        }

        "object is empty" in {

          when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(
            Some(
              Seq(
                ResponsiblePerson()
              )
            )
          )

          service.completeAndIncompleteRP(credId).futureValue.map(_._2) mustBe Some(Seq.empty[(ResponsiblePerson, Int)])
        }
      }

      "return incomplete responsible people" when {

        "objects are populated and status is not deleted" in {

          val people = Seq(
            ResponsiblePerson(Some(PersonName("James", None, "Smith"))),
            ResponsiblePerson(Some(PersonName("Jane", None, "Jones"))),
            ResponsiblePerson(Some(PersonName("Simon", None, "Johnson")))
          )

          when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(
            Some(people)
          )

          service.completeAndIncompleteRP(credId).futureValue.map(_._1) mustBe Some(Seq.empty[(ResponsiblePerson, Int)])
          service.completeAndIncompleteRP(credId).futureValue.map(_._2) mustBe Some(people.zipWithIndex.reverse)
        }
      }

      "return completed responsible people" when {

        "objects satisfy the completed predicate" in {

          val people = Seq(
            completeResponsiblePerson,
            completeResponsiblePerson.copy(personName = Some(PersonName("Laura", None, "Cole"))),
            completeResponsiblePerson.copy(personName = Some(PersonName("David", Some("Bradley"), "Philips")))
          )

          when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(
            Some(people)
          )

          service.completeAndIncompleteRP(credId).futureValue.map(_._1) mustBe Some(people.zipWithIndex.reverse)
          service.completeAndIncompleteRP(credId).futureValue.map(_._2) mustBe Some(Seq.empty[(ResponsiblePerson, Int)])
        }
      }

      "successfully partition complete and incomplete objects" in {

        val completePeople = Seq(
          completeResponsiblePerson,
          completeResponsiblePerson.copy(personName = Some(PersonName("Laura", None, "Cole"))),
          completeResponsiblePerson.copy(personName = Some(PersonName("David", Some("Bradley"), "Philips")))
        )

        val incompletePeople = Seq(
          ResponsiblePerson(Some(PersonName("James", None, "Smith"))),
          ResponsiblePerson(Some(PersonName("Jane", None, "Jones"))),
          ResponsiblePerson(Some(PersonName("Simon", None, "Johnson")))
        )

        when(mockCacheConnector.fetch[Seq[ResponsiblePerson]](any(), any())(any())) thenReturn Future.successful(
          Some(completePeople ++ incompletePeople)
        )

        service.completeAndIncompleteRP(credId).futureValue.map(_._1) mustBe Some(completePeople.zipWithIndex.reverse)
        service.completeAndIncompleteRP(credId).futureValue.map(_._2) mustBe Some(
          incompletePeople.zipWithIndex.reverse.map { case (rp, i) =>
            (rp, i + completePeople.length)
          }
        )
      }
    }
  }
}
