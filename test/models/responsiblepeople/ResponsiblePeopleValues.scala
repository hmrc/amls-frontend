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

package models.responsiblepeople

import controllers.responsiblepeople.NinoUtil
import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, ZeroToFiveMonths}
import play.api.libs.json.Json
import uk.gov.hmrc.domain.Nino
import utils.StatusConstants

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait ResponsiblePeopleValues extends NinoUtil {

  private val startDate          = Some(PositionStartDate(LocalDate.now()))
  private val formattedStartDate = startDate.get.startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  private val nino               = nextNino

  object DefaultValues {

    val residenceNonUk                  = NonUKResidence
    val residenceUk                     = UKResidence(Nino("AA111111A"))
    val residenceCountry                = Country("United Kingdom", "GB")
    val residenceNationality            = Country("United Kingdom", "GB")
    val currentPersonAddress            = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA111AA")
    val currentAddress                  = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
    val additionalPersonAddress         = PersonAddressUK("Line 1", Some("Line 2"), None, None, "AA11AA")
    val additionalAddress               = ResponsiblePersonAddress(additionalPersonAddress, Some(OneToThreeYears))
    val soleProprietorOfAnotherBusiness = SoleProprietorOfAnotherBusiness(true)
    // scalastyle:off magic.number
    val personName                      = PersonName("first", Some("middle"), "last")
    val legalName                       = PreviousName(Some(true), Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val noPreviousName                  = PreviousName(Some(false), None, None, None)
    val knownBy                         = KnownBy(Some(true), Some("name"))
    val noKnownBy                       = KnownBy(Some(false), None)
    val personResidenceTypeNonUk        =
      PersonResidenceType(residenceNonUk, Some(residenceCountry), Some(residenceNationality))
    val personResidenceTypeUk           = PersonResidenceType(residenceUk, Some(residenceCountry), Some(residenceNationality))
    val saRegistered                    = SaRegisteredYes("0123456789")
    val contactDetails                  = ContactDetails("07702743555", "test@test.com")
    val addressHistory                  = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val vatRegistered                   = VATRegisteredNo
    val training                        = TrainingYes("test")
    val experienceTraining              = ExperienceTrainingYes("Some training")
    val positions                       = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
    val ukPassportYes                   = UKPassportYes("000000000")
    val ukPassportNo                    = UKPassportNo
    val nonUKPassportYes                = NonUKPassportYes("87654321")
    val nonUKPassportNo                 = NoPassport
    val dateOfBirth                     = DateOfBirth(LocalDate.of(1990, 10, 2))
  }

  object NewValues {

    private val residence                  = UKResidence(Nino(nino))
    private val residenceCountry           = Country("United Kingdom", "GB")
    private val residenceNationality       = Country("United Kingdom", "GB")
    private val newPersonAddress           = PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("Spain", "ES"))
    private val newAdditionalPersonAddress =
      PersonAddressNonUK("Line 1", Some("Line 2"), None, None, Country("France", "FR"))
    private val currentAddress             = ResponsiblePersonCurrentAddress(newPersonAddress, Some(ZeroToFiveMonths))
    private val additionalAddress          = ResponsiblePersonAddress(newAdditionalPersonAddress, Some(ZeroToFiveMonths))

    val personName          = PersonName("firstnew", Some("middle"), "last")
    val legalName           = PreviousName(Some(true), Some("oldFirst"), Some("oldMiddle"), Some("oldLast"))
    val contactDetails      = ContactDetails("07000000000", "new@test.com")
    val addressHistory      = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val personResidenceType = PersonResidenceType(residence, Some(residenceCountry), Some(residenceNationality))
    val saRegistered        = SaRegisteredNo
    val vatRegistered       = VATRegisteredYes("12345678")
    val positions           = Positions(Set(Director, SoleProprietor), startDate)
    val experienceTraining  = ExperienceTrainingNo
    val training            = TrainingNo
  }

  val completeModelUkResidentFPtrue = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val incompleteModelUkResidentNoDOB = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResident = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = Some(DefaultValues.noKnownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(false)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldData = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentForOldDataNoPrevious = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = None,
    legalNameChangeDate = None,
    knownBy = None,
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val completeModelUkResidentNoPreviousName = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeUk),
    ukPassport = Some(DefaultValues.ukPassportYes),
    nonUKPassport = None,
    dateOfBirth = Some(DefaultValues.dateOfBirth),
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory),
    positions = Some(DefaultValues.positions),
    saRegistered = Some(DefaultValues.saRegistered),
    vatRegistered = Some(DefaultValues.vatRegistered),
    experienceTraining = Some(DefaultValues.experienceTraining),
    training = Some(DefaultValues.training),
    approvalFlags = ApprovalFlags(hasAlreadyPassedFitAndProper = Some(true), hasAlreadyPaidApprovalCheck = Some(true)),
    hasChanged = false,
    hasAccepted = false,
    lineId = Some(1),
    status = Some(StatusConstants.Unchanged),
    endDate = None,
    soleProprietorOfAnotherBusiness = Some(DefaultValues.soleProprietorOfAnotherBusiness)
  )

  val incompleteResponsiblePeople = ResponsiblePerson(
    personName = Some(DefaultValues.personName),
    legalName = Some(DefaultValues.legalName),
    legalNameChangeDate = Some(LocalDate.of(1990, 2, 24)),
    knownBy = Some(DefaultValues.knownBy),
    personResidenceType = Some(DefaultValues.personResidenceTypeNonUk),
    ukPassport = None,
    nonUKPassport = None,
    dateOfBirth = None,
    contactDetails = Some(DefaultValues.contactDetails),
    addressHistory = Some(DefaultValues.addressHistory)
  )

  val incompleteJsonCurrent = Json.obj(
    "personName"          -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "personResidenceType" -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"      -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"      -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      )
    )
  )

  val incompleteJsonCurrentUpToUkResident = Json.obj(
    "personName"          -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"           -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    )
  )

  val incompleteJsonCurrentUpToUkPassportNumber = Json.obj(
    "personName"          -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"           -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "ukPassport"          -> Json.obj(
      "ukPassport"       -> true,
      "ukPassportNumber" -> "000000000"
    )
  )

  val incompleteJsonCurrentUpToNonUkPassportNumber = Json.obj(
    "personName"          -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"           -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "nonUKPassport"       -> Json.obj(
      "nonUKPassport"       -> true,
      "nonUKPassportNumber" -> "87654321"
    )
  )

  val incompleteJsonCurrentUpToNoNonUkPassportDateOfBirth = Json.obj(
    "personName"          -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"           -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate" -> "1990-02-24",
    "personResidenceType" -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "nonUKPassport"       -> Json.obj(
      "nonUKPassport" -> false
    ),
    "dateOfBirth"         -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    )
  )

  val CompleteJsonPastNonUk = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"       -> false,
      "dateOfBirth"         -> "1990-10-02",
      "nonUKPassportNumber" -> "87654321",
      "countryOfBirth"      -> "GB",
      "nationality"         -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val incompleteJsonPastUk = Json.obj(
    "personName" -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    )
  )

  val CompleteJsonPastUk = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "personResidenceType"             -> Json.obj(
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeJsonPresentNonUkResidentUkPassport = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "ukPassport"                      -> Json.obj(
      "ukPassport"       -> true,
      "ukPassportNumber" -> "000000000"
    ),
    "dateOfBirth"                     -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeJsonPresentNonUkResidentNonUkPassport = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "knownBy"                         -> Json.obj(
      "hasOtherNames" -> true,
      "otherNames"    -> "name"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "ukPassport"                      -> Json.obj(
      "ukPassport" -> false
    ),
    "nonUKPassport"                   -> Json.obj(
      "nonUKPassport"       -> true,
      "nonUKPassportNumber" -> "87654321"
    ),
    "dateOfBirth"                     -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "approvalFlags"                   -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
    "hasChanged"                      -> false,
    "hasAccepted"                     -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged",
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    )
  )

  val completeJsonPresentNonUkResidentNoPassport = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "false",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "ukPassport"                      -> Json.obj(
      "ukPassport" -> false
    ),
    "nonUKPassport"                   -> Json.obj(
      "nonUKPassport" -> false
    ),
    "dateOfBirth"                     -> Json.obj(
      "dateOfBirth" -> "1990-10-02"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeJsonPresentUkResident = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "KnownBy"                         -> Json.obj(
      "hasOtherNames" -> true,
      "otherName"     -> "name"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "true",
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeJsonPresentUkResidentFitAndProper = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "KnownBy"                         -> Json.obj(
      "hasOtherNames" -> true,
      "otherName"     -> "name"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "true",
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "approvalFlags"                   -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeJsonPresentUkResidentFitAndProperApproval = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "legalName"                       -> Json.obj(
      "hasPreviousName" -> true,
      "firstName"       -> "oldFirst",
      "middleName"      -> "oldMiddle",
      "lastName"        -> "oldLast"
    ),
    "legalNameChangeDate"             -> "1990-02-24",
    "KnownBy"                         -> Json.obj(
      "hasOtherNames" -> true,
      "otherName"     -> "name"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "true",
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "approvalFlags"                   -> Json.obj("hasAlreadyPassedFitAndProper" -> true),
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeOldJsonPresentUkResident = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"    -> "first",
      "middleName"   -> "middle",
      "lastName"     -> "last",
      "previousName" -> Json.obj(
        "hasPreviousName" -> true,
        "firstName"       -> "oldFirst",
        "middleName"      -> "oldMiddle",
        "lastName"        -> "oldLast",
        "date"            -> "1990-02-24"
      )
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "true",
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeOldJsonPresentUkResidentNoPrevious = Json.obj(
    "personName"                      -> Json.obj(
      "firstName"  -> "first",
      "middleName" -> "middle",
      "lastName"   -> "last"
    ),
    "personResidenceType"             -> Json.obj(
      "isUKResidence"  -> "true",
      "nino"           -> "AA111111A",
      "countryOfBirth" -> "GB",
      "nationality"    -> "GB"
    ),
    "contactDetails"                  -> Json.obj(
      "phoneNumber"  -> "07702743555",
      "emailAddress" -> "test@test.com"
    ),
    "addressHistory"                  -> Json.obj(
      "currentAddress"    -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA111AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "01"
        )
      ),
      "additionalAddress" -> Json.obj(
        "personAddress" -> Json.obj(
          "personAddressLine1"    -> "Line 1",
          "personAddressLine2"    -> "Line 2",
          "personAddressPostCode" -> "AA11AA"
        ),
        "timeAtAddress" -> Json.obj(
          "timeAtAddress" -> "03"
        )
      )
    ),
    "positions"                       -> Json.obj(
      "positions" -> Seq("01", "03"),
      "startDate" -> formattedStartDate
    ),
    "saRegistered"                    -> Json.obj(
      "saRegistered" -> true,
      "utrNumber"    -> "0123456789"
    ),
    "vatRegistered"                   -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining"              -> Json.obj(
      "experienceTraining"    -> true,
      "experienceInformation" -> "Some training"
    ),
    "training"                        -> Json.obj(
      "training"    -> true,
      "information" -> "test"
    ),
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper"    -> true,
    "hasChanged"                      -> false,
    "lineId"                          -> 1,
    "status"                          -> "Unchanged"
  )

  val completeResponsiblePerson: ResponsiblePerson = ResponsiblePerson(
    personName = Some(PersonName("ANSTY", Some("EMIDLLE"), "DAVID")),
    legalName = Some(PreviousName(Some(false), None, None, None)),
    legalNameChangeDate = None,
    knownBy = Some(KnownBy(Some(false), None)),
    personResidenceType = Some(
      PersonResidenceType(
        NonUKResidence,
        Some(Country("Antigua and Barbuda", "bb")),
        Some(Country("United Kingdom", "GB"))
      )
    ),
    ukPassport = Some(UKPassportNo),
    nonUKPassport = Some(NoPassport),
    dateOfBirth = Some(DateOfBirth(LocalDate.of(1990, 2, 24))),
    contactDetails = Some(ContactDetails("0912345678", "TEST@EMAIL.COM")),
    addressHistory = Some(
      ResponsiblePersonAddressHistory(
        Some(
          ResponsiblePersonCurrentAddress(
            PersonAddressUK("add1", Some("add2"), Some("add3"), Some("add4"), "de4 5tg"),
            Some(OneToThreeYears),
            None
          )
        ),
        None,
        None
      )
    ),
    positions =
      Some(Positions(Set(NominatedOfficer, SoleProprietor), Some(PositionStartDate(LocalDate.of(2002, 2, 2))))),
    saRegistered = Some(SaRegisteredNo),
    vatRegistered = Some(VATRegisteredNo),
    experienceTraining = Some(ExperienceTrainingNo),
    training = Some(TrainingNo),
    approvalFlags = ApprovalFlags(Some(true), Some(true)),
    hasChanged = false,
    hasAccepted = true,
    lineId = Some(2),
    status = None,
    endDate = None,
    soleProprietorOfAnotherBusiness = None
  )
}
