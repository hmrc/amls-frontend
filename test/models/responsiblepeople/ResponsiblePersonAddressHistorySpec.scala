package models.responsiblepeople

import models.Country
import models.responsiblepeople.TimeAtAddress.{OneToThreeYears, SixToElevenMonths, ZeroToFiveMonths}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ResponsiblePersonAddressHistorySpec extends PlaySpec with MockitoSugar {

  val DefaultCurrentAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ"), ZeroToFiveMonths)
  val DefaultAdditionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE15GH"), SixToElevenMonths)
  val DefaultAdditionalExtraAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE1234"), OneToThreeYears)

  val NewCurrentAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES")), ZeroToFiveMonths)
  val NewAdditionalAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR")), ZeroToFiveMonths)
  val NewAdditionalExtraAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("UK", "UK")), SixToElevenMonths)

  val DefaultAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(DefaultCurrentAddress),
    additionalAddress = Some(DefaultAdditionalAddress),
    additionalExtraAddress = Some(DefaultAdditionalExtraAddress)
  )

  val NewAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(NewCurrentAddress),
    additionalAddress = Some(NewAdditionalAddress),
    additionalExtraAddress = Some(NewAdditionalExtraAddress)
  )

  "ResponsiblePersonAddressHistory" must {

    "update the model with current address" in {
      val updated = DefaultAddressHistory.currentAddress(NewCurrentAddress)
      updated.currentAddress must be(NewCurrentAddress)
    }

    "update the model with new additionalAddress" in {
      val updated = DefaultAddressHistory.additionalAddress(NewCurrentAddress)
      updated.additionalAddress must be(NewCurrentAddress)
    }

    "update the model with new additionalExtraAddress" in {
      val updated = DefaultAddressHistory.additionalExtraAddress(NewCurrentAddress)
      updated.additionalExtraAddress must be(NewCurrentAddress)
    }

    "validate complete json" must {

      val completeJson = Json.obj(
        "currentAddress" -> Json.obj(
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "NE981ZZ",
          "timeAtAddress" -> "01"
        ),
        "additionalAddress" -> Json.obj(
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "NE15GH",
          "timeAtAddress" -> "02"
        ),
        "additionalExtraAddress" -> Json.obj(
          "addressLine1" -> "Line 1",
          "addressLine2" -> "Line 2",
          "postCode" -> "NE1234",
          "timeAtAddress" -> "03"
        ))

      "Serialise as expected" in {
        Json.toJson(DefaultAddressHistory) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[ResponsiblePersonAddressHistory] must be(DefaultAddressHistory)
      }

    }

    "implicitly return an existing Model if one present" in {
      val responsiblePeople = ResponsiblePeople.default(Some(ResponsiblePeopleModel))
      responsiblePeople must be(ResponsiblePeopleModel)
    }

    "implicitly return an empty Model if not present" in {
      val responsiblePeople = ResponsiblePeople.default(None)
      responsiblePeople must be(ResponsiblePeople())
    }
  }

  "None" when {
    val initial: Option[ResponsiblePeople] = None

    "Merged with add person" must {
      "return ResponsiblePeople with correct add person" in {
        val result = initial.addPerson(DefaultAddPerson)
        result must be (ResponsiblePeople(Some(DefaultAddPerson)))
      }
    }

    "Merged with DefaultPersonResidenceType" must {
      "return ResponsiblePeople with correct DefaultPersonResidenceType" in {
        val result = initial.personResidenceType(DefaultPersonResidenceType)
        result must be (ResponsiblePeople(None, Some(DefaultPersonResidenceType)))
      }
    }

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.saRegistered(DefaultSaRegisteredYes)
        result must be (ResponsiblePeople(None, None, None, Some(DefaultSaRegisteredYes)))
      }
    }
  }

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {

      val initial = ResponsiblePeople(
        Some(DefaultAddPerson),
        Some(DefaultPersonResidenceType),
        Some(DefaultAddressHistory),
        Some(DefaultSaRegisteredYes)
      )

      initial.isComplete must be(true)
    }

    "the model is not complete" in {

      val initial = ResponsiblePeople(
        Some(DefaultAddPerson),
        Some(DefaultPersonResidenceType),
        Some(DefaultAddressHistory),
        None
      )

      initial.isComplete must be(false)

    }

    "the model address history is set but not completed" in {

      val PartialAddressHistory = ResponsiblePersonAddressHistory()

      val initial = ResponsiblePeople(
        Some(DefaultAddPerson),
        Some(DefaultPersonResidenceType),
        Some(PartialAddressHistory),
        Some(DefaultSaRegisteredYes)
      )

      initial.isComplete must be(false)

    }
  }

  "Merge with existing model" when {

    val initial = ResponsiblePeople(
      Some(DefaultAddPerson),
      Some(DefaultPersonResidenceType),
      Some(DefaultAddressHistory),
      Some(DefaultSaRegisteredYes))

    "Merged with add person" must {
      "return ResponsiblePeople with correct add person" in {
        val result = initial.addPerson(NewAddPerson)
        result must be (ResponsiblePeople(Some(NewAddPerson), Some(DefaultPersonResidenceType), Some(DefaultAddressHistory), Some(DefaultSaRegisteredYes)))
      }
    }

    "Merged with DefaultPersonResidenceType" must {
      "return ResponsiblePeople with correct DefaultPersonResidenceType" in {
        val result = initial.personResidenceType(NewPersonResidenceType)
        result must be (ResponsiblePeople(Some(DefaultAddPerson), Some(NewPersonResidenceType), Some(DefaultAddressHistory), Some(DefaultSaRegisteredYes)))
      }
    }

    "Merged with DefaultPreviousHomeAddress" must {
      "return ResponsiblePeople with correct DefaultPreviousHomeAddress" in {
        val result = initial.addressHistory(NewAddressHistory)
        result must be (ResponsiblePeople(Some(DefaultAddPerson), Some(DefaultPersonResidenceType), Some(NewAddressHistory), Some(DefaultSaRegisteredYes)))
      }
    }

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.saRegistered(NewSaRegisteredYes)
        result must be (ResponsiblePeople(Some(DefaultAddPerson), Some(DefaultPersonResidenceType), Some(DefaultAddressHistory), Some(NewSaRegisteredYes)))
      }
    }
  }