package models.responsiblepeople

import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ResponsiblePeopleSpec extends PlaySpec with MockitoSugar {

  val DefaultAddPerson = AddPerson("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)
  val DefaultPersonResidenceType = PersonResidenceType(UKResidence("AA3464646"), Country("United Kingdom", "GB"), Country("United Kingdom", "GB"))
  val DefaultSaRegisteredYes = SaRegisteredYes("0123456789")

  val DefaultCurrentAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ"), ZeroToFiveMonths)
  val DefaultAdditionalAddress = ResponsiblePersonAddress(PersonAddressUK("Line 1", "Line 2", None, None, "NE15GH"), ZeroToFiveMonths)

  val DefaultAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(DefaultCurrentAddress),
    additionalAddress = Some(DefaultAdditionalAddress)
  )
  val DefaultVatRegisteredNo = VATRegisteredNo

  val DefaultTraining = TrainingNo

  val DefaultPositions = Positions(Set(BeneficialOwner, InternalAccountant))

  val NewAddPerson = AddPerson("first", Some("middle"), "last", IsKnownByOtherNamesNo)

  val NewCurrentAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES")), ZeroToFiveMonths)
  val NewAdditionalAddress = ResponsiblePersonAddress(PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR")), ZeroToFiveMonths)

  val NewAddressHistory = ResponsiblePersonAddressHistory(
    currentAddress = Some(NewCurrentAddress),
    additionalAddress = Some(NewAdditionalAddress)
  )

  val NewPersonResidenceType = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
    Country("United Kingdom", "GB"), Country("United Kingdom", "GB"))
  val NewSaRegisteredYes = SaRegisteredNo
  val NewVatRegisteredYes = VATRegisteredYes("12345678")

  val NewPositions = Positions(Set(Director, SoleProprietor))

  val ResponsiblePeopleModel = ResponsiblePeople(
    addPerson = Some(DefaultAddPerson),
    addressHistory = Some(DefaultAddressHistory),
    positions = Some(DefaultPositions),
    saRegistered = Some(DefaultSaRegisteredYes)
  )

  "ResponsiblePeople" must {

    "update the model with the person" in {
      val addPersonUpdated = DefaultAddPerson.copy(firstName = "Johny")
      val newResponsiblePeople = ResponsiblePeopleModel.addPerson(addPersonUpdated)
      newResponsiblePeople.addPerson.get.firstName must be(addPersonUpdated.firstName)
    }

    "update the model with new address history" in {
      val newResponsiblePeople = ResponsiblePeopleModel.addressHistory(NewAddressHistory)
      newResponsiblePeople.addressHistory.fold(fail("No address found.")) { x => x must be (NewAddressHistory) }
    }

    "validate complete json" must {

      val completeJson = Json.obj(
        "addPerson" -> Json.obj(
          "firstName" -> "John",
          "middleName" -> "Envy",
          "lastName" -> "Doe",
          "isKnownByOtherNames" -> false
        ),
        "addressHistory" -> Json.obj(
          "currentAddress" -> Json.obj(
            "personAddress" -> Json.obj(
              "personAddressLine1" -> "Line 1",
              "personAddressLine2" -> "Line 2",
              "personAddressPostCode" -> "NE981ZZ"
            ),
            "timeAtAddress" -> Json.obj(
              "timeAtAddress" -> "01"
            )
          ),
          "additionalAddress" -> Json.obj(
            "personAddress" -> Json.obj(
              "personAddressLine1" -> "Line 1",
              "personAddressLine2" -> "Line 2",
              "personAddressPostCode" -> "NE15GH"
            ),
            "timeAtAddress" -> Json.obj(
              "timeAtAddress" -> "01"
            )
          )
        ),
        "positions" -> Json.obj(
          "positions" -> Seq("01", "03")
        ),
        "saRegistered" -> Json.obj(
          "saRegistered" -> true,
          "utrNumber" -> "0123456789"
        )
      )

      "Serialise as expected" in {
        Json.toJson(ResponsiblePeopleModel) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[ResponsiblePeople] must be(ResponsiblePeopleModel)
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

    "Merged with DefaultPositions" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.positions(DefaultPositions)
        result must be (ResponsiblePeople(None, None, None, Some(DefaultPositions), None))
      }
    }

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.saRegistered(DefaultSaRegisteredYes)
        result must be (ResponsiblePeople(None, None, None, None, Some(DefaultSaRegisteredYes)))
      }
    }


    "Merged with VatRegistered" must {
      "return ResponsiblePeople with correct VatRegistered" in {
        val result = initial.vatRegistered(DefaultVatRegisteredNo)
        result must be (ResponsiblePeople(vatRegistered = Some(DefaultVatRegisteredNo)))
      }
    }
  }

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {

      val initial = ResponsiblePeople(
        Some(DefaultAddPerson),
        Some(DefaultPersonResidenceType),
        Some(DefaultAddressHistory),
        Some(DefaultPositions),
        Some(DefaultSaRegisteredYes),
        Some(DefaultVatRegisteredNo),
        Some(DefaultTraining)
      )

      initial.isComplete must be(true)
    }

    "the model is not complete" in {

      val initial = ResponsiblePeople(
        Some(DefaultAddPerson),
        Some(DefaultPersonResidenceType),
        Some(DefaultAddressHistory),
        Some(DefaultPositions),
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
        Some(DefaultPositions),
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
      Some(DefaultPositions),
      Some(DefaultSaRegisteredYes),
      Some(DefaultVatRegisteredNo))

    "Merged with add person" must {
      "return ResponsiblePeople with correct NewAddPerson" in {
        val result = initial.addPerson(NewAddPerson)

        result must be (ResponsiblePeople(
          Some(NewAddPerson),
          Some(DefaultPersonResidenceType),
          Some(DefaultAddressHistory),
          Some(DefaultPositions),
          Some(DefaultSaRegisteredYes),
          Some(DefaultVatRegisteredNo)))
      }
    }

    "Merged with DefaultPersonResidenceType" must {
      "return ResponsiblePeople with correct NewPersonResidenceType" in {
        val result = initial.personResidenceType(NewPersonResidenceType)
        result must be (ResponsiblePeople(
          Some(DefaultAddPerson),
          Some(NewPersonResidenceType),
          Some(DefaultAddressHistory),
          Some(DefaultPositions),
          Some(DefaultSaRegisteredYes),
          Some(DefaultVatRegisteredNo)))
      }
    }

    "Merged with DefaultAddressHistory" must {
      "return ResponsiblePeople with correct NewAddressHistory" in {
        val result = initial.addressHistory(NewAddressHistory)
        result must be (ResponsiblePeople(
          Some(DefaultAddPerson),
          Some(DefaultPersonResidenceType),
          Some(NewAddressHistory),
          Some(DefaultPositions),
          Some(DefaultSaRegisteredYes),
          Some(DefaultVatRegisteredNo)))
      }
    }

    "Merged with DefaultPositions" must {
      "return ResponsiblePeople with correct NewPositions" in {
        val result = initial.positions(NewPositions)
        result must be (ResponsiblePeople(
          Some(DefaultAddPerson),
          Some(DefaultPersonResidenceType),
          Some(DefaultAddressHistory),
          Some(NewPositions),
          Some(DefaultSaRegisteredYes),
          Some(DefaultVatRegisteredNo)))
      }
    }

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct NewSaRegisteredYes" in {
        val result = initial.saRegistered(NewSaRegisteredYes)
        result must be (ResponsiblePeople(
          Some(DefaultAddPerson),
          Some(DefaultPersonResidenceType),
          Some(DefaultAddressHistory),
          Some(DefaultPositions),
          Some(NewSaRegisteredYes),
          Some(DefaultVatRegisteredNo)))
      }
    }

    "Merged with VATRegistered" must {
      "return ResponsiblePeople with correct NewVatRegisteredYes" in {
        val result = initial.vatRegistered(NewVatRegisteredYes)
        result must be (ResponsiblePeople(
          Some(DefaultAddPerson),
          Some(DefaultPersonResidenceType),
          Some(DefaultAddressHistory),
          Some(DefaultPositions),
          Some(DefaultSaRegisteredYes),
          Some(NewVatRegisteredYes)))
      }
    }
  }
}
