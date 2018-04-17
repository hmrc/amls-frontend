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
}
