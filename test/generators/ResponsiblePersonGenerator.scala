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

package generators

import models.WithName
import models.responsiblepeople.TimeAtAddress.ThreeYearsPlus
import models.responsiblepeople._
import org.scalacheck.Gen

import java.time.LocalDate

// scalastyle:off magic.number
trait ResponsiblePersonGenerator extends BaseGenerator {

  val positionInBusinessGen: Gen[Seq[WithName with PositionWithinBusiness]] =
    Gen.someOf(
      BeneficialOwner,
      InternalAccountant,
      Director,
      NominatedOfficer,
      Partner,
      SoleProprietor,
      DesignatedMember
    )

  val positionsGen: Gen[Positions] = for {
    positions <- positionInBusinessGen
  } yield Positions(positions.toSet, Some(PositionStartDate(LocalDate.now())))

  private val maxNameTypeLength = 35

  val personNameGen: Gen[PersonName] = for {
    firstName <- stringOfLengthGen(maxNameTypeLength)
    lastName  <- stringOfLengthGen(maxNameTypeLength)
  } yield PersonName(firstName, None, lastName)

  val personAddressGen: Gen[PersonAddress] = for {
    line1    <- stringOfLengthGen(10)
    line2    <- stringOfLengthGen(10)
    postCode <- postcodeGen
  } yield PersonAddressUK(line1, Some(line2), None, None, postCode)

  val responsiblePersonGen: Gen[ResponsiblePerson] = for {
    personName  <- personNameGen
    positions   <- positionsGen
    phoneNumber <- numSequence(10)
    email       <- emailGen
    address     <- personAddressGen
  } yield ResponsiblePerson(
    personName = Some(personName),
    legalName = Some(PreviousName(hasPreviousName = Some(false), None, None, None)),
    legalNameChangeDate = None,
    knownBy = Some(KnownBy(Some(false), None)),
    personResidenceType = Some(PersonResidenceType(NonUKResidence, None, None)),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(ContactDetails(phoneNumber, email)),
    addressHistory =
      Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(address, Some(ThreeYearsPlus), None)))),
    positions = Some(positions),
    saRegistered = Some(SaRegisteredNo),
    vatRegistered = None,
    experienceTraining = Some(ExperienceTrainingNo),
    training = Some(TrainingNo),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = None),
    hasChanged = false,
    hasAccepted = true,
    lineId = None,
    status = None,
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false))
  )

  val completeResponsiblePersonGen: Gen[ResponsiblePerson] = for {
    personName  <- personNameGen
    positions   <- positionsGen
    phoneNumber <- numSequence(10)
    email       <- emailGen
    address     <- personAddressGen
  } yield new ResponsiblePerson(
    Some(personName),
    Some(PreviousName(hasPreviousName = Some(false), None, None, None)),
    None,
    Some(KnownBy(Some(false), None)),
    Some(PersonResidenceType(NonUKResidence, None, None)),
    None,
    None,
    None,
    Some(ContactDetails(phoneNumber, email)),
    Some(ResponsiblePersonAddressHistory(Some(ResponsiblePersonCurrentAddress(address, Some(ThreeYearsPlus), None)))),
    Some(positions),
    Some(SaRegisteredNo),
    None,
    Some(ExperienceTrainingNo),
    Some(TrainingNo),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = None),
    hasChanged = false,
    hasAccepted = true,
    None,
    None,
    None,
    Some(SoleProprietorOfAnotherBusiness(false))
  ) {
    override def isComplete: Boolean = true
  }

  def responsiblePersonWithPositionsGen(positions: Option[Set[PositionWithinBusiness]]): Gen[ResponsiblePerson] = for {
    person <- responsiblePersonGen
  } yield person.copy(positions = positions.fold[Option[Positions]](None)(p => Some(Positions(p, None))))

  def responsiblePeopleGen(i: Int): Gen[List[ResponsiblePerson]] =
    Gen.listOfN[ResponsiblePerson](i, responsiblePersonGen)

}
