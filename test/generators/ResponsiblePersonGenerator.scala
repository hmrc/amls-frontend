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

package generators

import models.responsiblepeople._
import org.scalacheck.Gen
import models.FormTypes
import org.joda.time.LocalDate

trait ResponsiblePersonGenerator extends BaseGenerator {

  val positionInBusinessGen =
    Gen.someOf(
      BeneficialOwner,
      InternalAccountant,
      Director,
      NominatedOfficer,
      Partner,
      SoleProprietor,
      DesignatedMember
    )

  val positionsGen = for {
    positions <- positionInBusinessGen
  } yield Positions(positions.toSet, Some(new LocalDate()))

  val personNameGen: Gen[PersonName] = for {
    firstName <- stringOfLengthGen(FormTypes.maxNameTypeLength)
    lastName <- stringOfLengthGen(FormTypes.maxNameTypeLength)
  } yield PersonName(firstName, None, lastName, None, None)

  val responsiblePersonGen: Gen[ResponsiblePeople] = for {
    personName <- personNameGen
    positions <- positionsGen
  } yield ResponsiblePeople(Some(personName), positions = Some(positions))

  def responsiblePersonWithPositionsGen(positions: Option[Set[PositionWithinBusiness]]): Gen[ResponsiblePeople] = for {
    person <- responsiblePersonGen
  } yield person.copy(positions = positions.fold[Option[Positions]](None)(p => Some(Positions(p, None))))

}
