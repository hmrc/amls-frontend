/*
 * Copyright 2017 HM Revenue & Customs
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

import models.responsiblepeople.{Partner, Positions, ResponsiblePeople}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec


class DeclarationHelperSpec extends PlaySpec with MustMatchers {

  "numberOfPartners" must {
    "return 0 when there are no partners" in {

      DeclarationHelper.numberOfPartners(Seq(ResponsiblePeople())) mustBe 0

    }

    "return the correct number when there are one or more partners" in {

      DeclarationHelper.numberOfPartners(Seq(ResponsiblePeople(
        positions = Some(Positions(Set(Partner), None))
      ))) mustBe 1

      DeclarationHelper.numberOfPartners(Seq(
        ResponsiblePeople(positions = Some(Positions(Set(Partner), None))),
        ResponsiblePeople(positions = Some(Positions(Set(Partner), None)))
      )) mustBe 2
    }

    "not count responsible people whose status is Deleted" in {

      DeclarationHelper.numberOfPartners(Seq(ResponsiblePeople(
        positions = Some(Positions(Set(Partner), None)),
        status = Some(StatusConstants.Deleted)
      ))) mustBe 0

      DeclarationHelper.numberOfPartners(Seq(
        ResponsiblePeople(
          positions = Some(Positions(Set(Partner), None)),
          status = Some(StatusConstants.Deleted)
        ),
        ResponsiblePeople(
          positions = Some(Positions(Set(Partner), None)),
          status = Some(StatusConstants.Deleted)
        ),
        ResponsiblePeople(
          positions = Some(Positions(Set(Partner), None))
        )
      )) mustBe 1
    }
  }
}
