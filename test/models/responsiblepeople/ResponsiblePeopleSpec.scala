package models.responsiblepeople

import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => meq}


class ResponsiblePeopleSpec extends PlaySpec with MockitoSugar with ResponsiblePeopleValues {

  "ResponsiblePeople" must {

    "validate complete json" must {

      "Serialise as expected" in {
        Json.toJson(CompleteResponsiblePeople) must be(CompleteJson)
      }

      "Deserialise as expected" in {
        CompleteJson.as[ResponsiblePeople] must be(CompleteResponsiblePeople)
      }
    }

    "implicitly return an existing Model if one present" in {
      val responsiblePeople = ResponsiblePeople.default(Some(CompleteResponsiblePeople))
      responsiblePeople must be(CompleteResponsiblePeople)
    }

    "implicitly return an empty Model if not present" in {
      val responsiblePeople = ResponsiblePeople.default(None)
      responsiblePeople must be(ResponsiblePeople())
    }
  }

  it when {
    "the section has not been started" must {
      "direct the user to the add controller with what you need guidance requested" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(None)

        ResponsiblePeople.section(mockCacheMap).call must be (controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
      }
    }

    "the section is complete" must {
      "direct the user to the summary page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(CompleteResponsiblePeople)))

        ResponsiblePeople.section(mockCacheMap).call must be (controllers.responsiblepeople.routes.YourAnswersController.get())
      }
    }

    "the section is partially complete" must {
      "direct the user to the start of the the journey at the correct index for the incomplete item" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(CompleteResponsiblePeople, CompleteResponsiblePeople, InCompleteResponsiblePeople)))

        ResponsiblePeople.section(mockCacheMap).call must be (controllers.responsiblepeople.routes.WhoMustRegisterController.get(3))

      }
    }
  }

  "None" when {

    val EmptyResponsiblePeople: Option[ResponsiblePeople] = None

    "Merged with personName" must {
      "return ResponsiblePeople with correct personName" in {
        val result = EmptyResponsiblePeople.personName(NewValues.personName)
        result must be (ResponsiblePeople(personName = Some(NewValues.personName)))
      }
    }

    "Merged with PersonResidenceType" must {
      "return ResponsiblePeople with correct PersonResidenceType" in {
        val result = EmptyResponsiblePeople.personResidenceType(NewValues.personResidenceType)
        result must be (ResponsiblePeople(personResidenceType = Some(NewValues.personResidenceType)))
      }
    }

    "Merged with ContactDetails" must {
      "return ResponsiblePeople with correct ContactDetails" in {
        val result = EmptyResponsiblePeople.contactDetails(NewValues.contactDetails)
        result must be (ResponsiblePeople(contactDetails = Some(NewValues.contactDetails)))
      }
    }

    "Merged with AddressHistory" must {
      "return ResponsiblePeople with correct AddressHistory" in {
        val result = EmptyResponsiblePeople.addressHistory(NewValues.addressHistory)
        result must be (ResponsiblePeople(addressHistory = Some(NewValues.addressHistory)))
      }
    }


    "Merged with Positions" must {
      "return ResponsiblePeople with correct Positions" in {
        val result = EmptyResponsiblePeople.positions(NewValues.positions)
        result must be (ResponsiblePeople(positions = Some(NewValues.positions)))
      }
    }

    "Merged with SaRegistered" must {
      "return ResponsiblePeople with correct SaRegistered" in {
        val result = EmptyResponsiblePeople.saRegistered(NewValues.saRegistered)
        result must be (ResponsiblePeople(saRegistered = Some(NewValues.saRegistered)))
      }
    }

    "Merged with VatRegistered" must {
      "return ResponsiblePeople with correct VatRegistered" in {
        val result = EmptyResponsiblePeople.vatRegistered(NewValues.vatRegistered)
        result must be (ResponsiblePeople(vatRegistered = Some(NewValues.vatRegistered)))
      }
    }

    "Merged with experienceTraining" must {
      "return ResponsiblePeople with correct experienceTraining" in {
        val result = EmptyResponsiblePeople.experienceTraining(NewValues.experienceTraining)
        result must be (ResponsiblePeople(experienceTraining = Some(NewValues.experienceTraining)))
      }
    }

    "Merged with Training" must {
      "return ResponsiblePeople with correct Training" in {
        val result = EmptyResponsiblePeople.training(NewValues.training)
        result must be (ResponsiblePeople(training = Some(NewValues.training)))
      }
    }

    "Merged with FitAndProper" must {
      "return ResponsiblePeople with correct hasAlreadyPassedFitAndProper" in {
        val result = EmptyResponsiblePeople.hasAlreadyPassedFitAndProper(true)
        result must be (ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true)))
      }
    }

  }

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {
      CompleteResponsiblePeople.isComplete must be(true)
    }

    "the model is not complete" in {
      val initial = ResponsiblePeople()
      initial.isComplete must be(false)
    }

  }

  "Merge with existing model" when {

    "Merged with add personName" must {
      "return ResponsiblePeople with correct personName" in {
        val result = CompleteResponsiblePeople.personName(NewValues.personName)
        result must be (ResponsiblePeople(
          Some(NewValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with PersonResidenceType" must {
      "return ResponsiblePeople with correct PersonResidenceType" in {
        val result = CompleteResponsiblePeople.personResidenceType(NewValues.personResidenceType)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(NewValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with ContactDetails" must {
      "return ResponsiblePeople with correct ContactDetails" in {
        val result = CompleteResponsiblePeople.contactDetails(NewValues.contactDetails)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(NewValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with AddressHistory" must {
      "return ResponsiblePeople with correct AddressHistory" in {
        val result = CompleteResponsiblePeople.addressHistory(NewValues.addressHistory)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(NewValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with Positions" must {
      "return ResponsiblePeople with correct Positions" in {
        val result = CompleteResponsiblePeople.positions(NewValues.positions)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(NewValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with SaRegistered" must {
      "return ResponsiblePeople with correct SaRegistered" in {
        val result = CompleteResponsiblePeople.saRegistered(NewValues.saRegistered)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(NewValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with VatRegistered" must {
      "return ResponsiblePeople with correct VatRegistered" in {
        val result = CompleteResponsiblePeople.vatRegistered(NewValues.vatRegistered)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(NewValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with experienceTraining" must {
      "return ResponsiblePeople with correct experienceTraining" in {
        val result = CompleteResponsiblePeople.experienceTraining(NewValues.experienceTraining)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(NewValues.experienceTraining),
          Some(DefaultValues.training),
          Some(true)))
      }
    }

    "Merged with Training" must {
      "return ResponsiblePeople with correct Training" in {
        val result = CompleteResponsiblePeople.training(NewValues.training)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(NewValues.training),
          Some(true)))
      }
    }

    "Merged with hasAlreadyPassedFitAndProper" must {
      "return ResponsiblePeople with correct FitAndProper Value" in {
        val result = CompleteResponsiblePeople.hasAlreadyPassedFitAndProper(false)
        result must be (ResponsiblePeople(
          Some(DefaultValues.personName),
          Some(DefaultValues.personResidenceType),
          Some(DefaultValues.contactDetails),
          Some(DefaultValues.addressHistory),
          Some(DefaultValues.positions),
          Some(DefaultValues.saRegistered),
          Some(DefaultValues.vatRegistered),
          Some(DefaultValues.experienceTraining),
          Some(DefaultValues.training),
          Some(false)))
      }
    }
  }
}

trait ResponsiblePeopleValues {

  import DefaultValues._

  object DefaultValues {

    private val residence = UKResidence("AA3464646")
    private val residenceCountry = Country("United Kingdom", "GB")
    private val residenceNationality = Country("United Kingdom", "GB")
    private val currentPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ")
    private val currentAddress = ResponsiblePersonAddress(currentPersonAddress, ZeroToFiveMonths)
    private val additionalPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE15GH")
    private val additionalAddress = ResponsiblePersonAddress(additionalPersonAddress, ZeroToFiveMonths)
    //scalastyle:off magic.number
    val previousName = PreviousName(Some("Matt"), Some("Mc"), Some("Fly"), new LocalDate(1990, 2, 24))
    val personName = PersonName("John", Some("Envy"), "Doe", Some(previousName), Some("name"))
    val personResidenceType = PersonResidenceType(residence, residenceCountry, residenceNationality)
    val saRegistered = SaRegisteredYes("0123456789")
    val contactDetails = ContactDetails("07702743555", "test@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val vatRegistered = VATRegisteredNo
    val training = TrainingYes("test")
    val experienceTraining = ExperienceTrainingYes("Some training")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant))
  }

  object NewValues {

    private val residenceYear = 1990
    private val residenceMonth = 2
    private val residenceDay = 24
    private val residenceDate = new LocalDate(residenceYear, residenceMonth, residenceDay)
    private val residence = NonUKResidence(residenceDate, UKPassport("123464646"))
    private val residenceCountry = Country("United Kingdom", "GB")
    private val residenceNationality = Country("United Kingdom", "GB")
    private val newPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("Spain", "ES"))
    private val newAdditionalPersonAddress = PersonAddressNonUK("Line 1", "Line 2", None, None, Country("France", "FR"))
    private val currentAddress = ResponsiblePersonAddress(newPersonAddress, ZeroToFiveMonths)
    private val additionalAddress = ResponsiblePersonAddress(newAdditionalPersonAddress, ZeroToFiveMonths)

    val personName = PersonName("first", Some("middle"), "last", None, None)
    val contactDetails = ContactDetails("07702743444", "new@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val personResidenceType = PersonResidenceType(residence, residenceCountry, residenceNationality)
    val saRegistered = SaRegisteredNo
    val vatRegistered = VATRegisteredYes("12345678")
    val positions = Positions(Set(Director, SoleProprietor))
    val experienceTraining = ExperienceTrainingNo
    val training = TrainingNo
  }

  val CompleteResponsiblePeople = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.personResidenceType),
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory),
    Some(DefaultValues.positions),
    Some(DefaultValues.saRegistered),
    Some(DefaultValues.vatRegistered),
    Some(DefaultValues.experienceTraining),
    Some(DefaultValues.training),
    Some(true)
  )

  val InCompleteResponsiblePeople = ResponsiblePeople(
    Some(DefaultValues.personName),
    Some(DefaultValues.personResidenceType),
    Some(DefaultValues.contactDetails),
    Some(DefaultValues.addressHistory)
  )

  val CompleteJson = Json.obj(
    "personName" -> Json.obj(
      "firstName" -> "John",
      "middleName" -> "Envy",
      "lastName" -> "Doe",
      "previousName" -> Json.obj(
            "firstName" -> "Matt",
            "middleName" -> "Mc",
            "lastName" -> "Fly",
            "date" -> "1990-02-24"
          ),
      "otherNames" -> "name"
    ),
    "personResidenceType" -> Json.obj(
      "nino" -> "AA3464646",
      "countryOfBirth" -> "GB",
      "nationality" -> "GB"
    ),
    "contactDetails" -> Json.obj(
      "phoneNumber" -> "07702743555",
      "emailAddress" -> "test@test.com"
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
    ),
    "vatRegistered" -> Json.obj(
      "registeredForVAT" -> false
    ),
    "experienceTraining" -> Json.obj(
      "experienceTraining" -> true,
      "experienceInformation" -> "Some training"
    ),
    "training" -> Json.obj(
      "training" -> true,
      "information" -> "test"
    ),
    "hasAlreadyPassedFitAndProper" -> true
  )

  /** Make sure Responsible People model is complete */
  assert(CompleteResponsiblePeople.isComplete)

}