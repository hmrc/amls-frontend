package models.responsiblepeople

import models.Country
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.joda.time.{DateTimeUtils, LocalDate}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => meq}
import utils.StatusConstants


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

        ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.ResponsiblePeopleAddController.get(true))
      }
    }

    "the section is complete" must {
      "direct the user to the summary page" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(CompleteResponsiblePeople)))

        ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.YourAnswersController.get())
      }
    }

    "the section is partially complete" must {
      "direct the user to the start of the the journey at the correct index for the incomplete item" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(CompleteResponsiblePeople, CompleteResponsiblePeople, InCompleteResponsiblePeople)))

        ResponsiblePeople.section(mockCacheMap).call must be(controllers.responsiblepeople.routes.WhoMustRegisterController.get(3))

      }
    }

    "the section consistes of just 1 empty Responsible Person" must {
      "return a result indicating NotStarted" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(ResponsiblePeople())))

        ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.NotStarted)
      }
    }

    "the section consists of a partially complete model followed by a completely empty one" must {
      "return a result indicating partial completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(InCompleteResponsiblePeople, ResponsiblePeople())))

        ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.Started)
      }
    }

    "the section consists of a complete model followed by an empty one" must {
      "return a result indicating completeness" in {
        val mockCacheMap = mock[CacheMap]

        when(mockCacheMap.getEntry[Seq[ResponsiblePeople]](meq(ResponsiblePeople.key))(any()))
          .thenReturn(Some(Seq(CompleteResponsiblePeople, ResponsiblePeople())))

        ResponsiblePeople.section(mockCacheMap).status must be(models.registrationprogress.Completed)
      }
    }
  }

  "None" when {

    val EmptyResponsiblePeople: Option[ResponsiblePeople] = None

    "Merged with personName" must {
      "return ResponsiblePeople with correct personName" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).personName(NewValues.personName)
        result must be(ResponsiblePeople(personName = Some(NewValues.personName), hasChanged = true))
      }
    }

    "Merged with PersonResidenceType" must {
      "return ResponsiblePeople with correct PersonResidenceType" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).personResidenceType(NewValues.personResidenceType)
        result must be(ResponsiblePeople(personResidenceType = Some(NewValues.personResidenceType), hasChanged = true))
      }
    }

    "Merged with ContactDetails" must {
      "return ResponsiblePeople with correct ContactDetails" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).contactDetails(NewValues.contactDetails)
        result must be(ResponsiblePeople(contactDetails = Some(NewValues.contactDetails), hasChanged = true))
      }
    }

    "Merged with AddressHistory" must {
      "return ResponsiblePeople with correct AddressHistory" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).addressHistory(NewValues.addressHistory)
        result must be(ResponsiblePeople(addressHistory = Some(NewValues.addressHistory), hasChanged = true))
      }
    }


    "Merged with Positions" must {
      "return ResponsiblePeople with correct Positions" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).positions(NewValues.positions)
        result must be(ResponsiblePeople(positions = Some(NewValues.positions), hasChanged = true))
      }
    }

    "Merged with SaRegistered" must {
      "return ResponsiblePeople with correct SaRegistered" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).saRegistered(NewValues.saRegistered)
        result must be(ResponsiblePeople(saRegistered = Some(NewValues.saRegistered), hasChanged = true))
      }
    }

    "Merged with VatRegistered" must {
      "return ResponsiblePeople with correct VatRegistered" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).vatRegistered(NewValues.vatRegistered)
        result must be(ResponsiblePeople(vatRegistered = Some(NewValues.vatRegistered), hasChanged = true))
      }
    }

    "Merged with experienceTraining" must {
      "return ResponsiblePeople with correct experienceTraining" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).experienceTraining(NewValues.experienceTraining)
        result must be(ResponsiblePeople(experienceTraining = Some(NewValues.experienceTraining), hasChanged = true))
      }
    }

    "Merged with Training" must {
      "return ResponsiblePeople with correct Training" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).training(NewValues.training)
        result must be(ResponsiblePeople(training = Some(NewValues.training), hasChanged = true))
      }
    }

    "Merged with FitAndProper" must {
      "return ResponsiblePeople with correct hasAlreadyPassedFitAndProper" in {
        val result = ResponsiblePeople.default(EmptyResponsiblePeople).hasAlreadyPassedFitAndProper(true)
        result must be(ResponsiblePeople(hasAlreadyPassedFitAndProper = Some(true), hasChanged = true))
      }
    }

  }

  "Successfully validate if the model is complete" when {

    "the model is fully complete" in {
      CompleteResponsiblePeople.isComplete must be(true)
    }

    "the model partially complete with soleProprietorOfAnotherBusiness is empty" in {
      CompleteResponsiblePeople.copy(soleProprietorOfAnotherBusiness = None).isComplete must be(true)
    }

    "the model partially complete with vat registration model is empty" in {
      CompleteResponsiblePeople.copy(vatRegistered = None).isComplete must be(false)
    }

    "the model partially complete soleProprietorOfAnotherBusiness is selected as No vat registration is not empty" in {
      CompleteResponsiblePeople.copy(soleProprietorOfAnotherBusiness = Some(SoleProprietorOfAnotherBusiness(false)),
        vatRegistered = Some(VATRegisteredNo)).isComplete must be(false)
    }

    "the model is has no data" in {
      val initial = ResponsiblePeople()
      initial.isComplete must be(true)
    }

    "the model is not complete" in {
      val initial = ResponsiblePeople(Some(DefaultValues.personName))
      initial.isComplete must be(false)
    }

  }

  "ResponsiblePeople class" when {
    "personName value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.personName(DefaultValues.personName)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.personName(NewValues.personName)
          result must be(CompleteResponsiblePeople.copy(personName = Some(NewValues.personName), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "personResidenceType value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.personResidenceType(DefaultValues.personResidenceType)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.personResidenceType(NewValues.personResidenceType)
          result must be(CompleteResponsiblePeople.copy(personResidenceType = Some(NewValues.personResidenceType), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "contactDetails value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.contactDetails(DefaultValues.contactDetails)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.contactDetails(NewValues.contactDetails)
          result must be(CompleteResponsiblePeople.copy(contactDetails = Some(NewValues.contactDetails), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "addressHistory value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.addressHistory(DefaultValues.addressHistory)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.addressHistory(NewValues.addressHistory)
          result must be(CompleteResponsiblePeople.copy(addressHistory = Some(NewValues.addressHistory), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "positions value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.positions(DefaultValues.positions)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.positions(NewValues.positions)
          result must be(CompleteResponsiblePeople.copy(positions = Some(NewValues.positions), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "saRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.saRegistered(DefaultValues.saRegistered)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.saRegistered(NewValues.saRegistered)
          result must be(CompleteResponsiblePeople.copy(saRegistered = Some(NewValues.saRegistered), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "vatRegistered value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.vatRegistered(DefaultValues.vatRegistered)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.vatRegistered(NewValues.vatRegistered)
          result must be(CompleteResponsiblePeople.copy(vatRegistered = Some(NewValues.vatRegistered), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "experienceTraining value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.experienceTraining(DefaultValues.experienceTraining)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.experienceTraining(NewValues.experienceTraining)
          result must be(CompleteResponsiblePeople.copy(experienceTraining = Some(NewValues.experienceTraining), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "training value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.training(DefaultValues.training)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.training(NewValues.training)
          result must be(CompleteResponsiblePeople.copy(training = Some(NewValues.training), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "hasAlreadyPassedFitAndProper value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.hasAlreadyPassedFitAndProper(true)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.hasAlreadyPassedFitAndProper(false)
          result must be(CompleteResponsiblePeople.copy(hasAlreadyPassedFitAndProper = Some(false), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
    "status value is set" which {
      "is the same as before" must {
        "leave the object unchanged" in {
          val result = CompleteResponsiblePeople.status(StatusConstants.Unchanged)
          result must be(CompleteResponsiblePeople)
          result.hasChanged must be(false)
        }
      }

      "is different" must {
        "set the hasChanged & previouslyRegisterd Properties" in {
          val result = CompleteResponsiblePeople.status(StatusConstants.Deleted)
          result must be(CompleteResponsiblePeople.copy(status = Some(StatusConstants.Deleted), hasChanged = true))
          result.hasChanged must be(true)
        }
      }
    }
  }

  "anyChanged" must {
    val originalResponsiblePeople = Seq(CompleteResponsiblePeople)
    val responsiblePeopleChanged = Seq(CompleteResponsiblePeople.copy(hasChanged=true))
    val responsiblePeopleDeleted = Seq(CompleteResponsiblePeople.copy(status=Some(StatusConstants.Deleted)))

    "return false" when {
      "no ResponsiblePeople within the sequence have changed" in {
        val res = ResponsiblePeople.anyChanged(originalResponsiblePeople)
        res must be(false)
      }
    }
    "return true" when {
      "at least one ResponsiblePeople within the sequence has changed" in {
        val res = ResponsiblePeople.anyChanged(responsiblePeopleChanged)
        res must be(true)
      }
    }
  }
}

trait ResponsiblePeopleValues {

  import DefaultValues._



  private val startDate = Some(new LocalDate())

  object DefaultValues {

    private val residence = UKResidence("AA3464646")
    private val residenceCountry = Country("United Kingdom", "GB")
    private val residenceNationality = Country("United Kingdom", "GB")
    private val currentPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE981ZZ")
    private val currentAddress = ResponsiblePersonCurrentAddress(currentPersonAddress, Some(ZeroToFiveMonths))
    private val additionalPersonAddress = PersonAddressUK("Line 1", "Line 2", None, None, "NE15GH")
    private val additionalAddress = ResponsiblePersonAddress(additionalPersonAddress, Some(ZeroToFiveMonths))
    val soleProprietorOfAnotherBusiness = SoleProprietorOfAnotherBusiness(true)
    //scalastyle:off magic.number
    val previousName = PreviousName(Some("Matt"), Some("Mc"), Some("Fly"), new LocalDate(1990, 2, 24))
    val personName = PersonName("John", Some("Envy"), "Doe", Some(previousName), Some("name"))
    val personResidenceType = PersonResidenceType(residence, residenceCountry, Some(residenceNationality))
    val saRegistered = SaRegisteredYes("0123456789")
    val contactDetails = ContactDetails("07702743555", "test@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val vatRegistered = VATRegisteredNo
    val training = TrainingYes("test")
    val experienceTraining = ExperienceTrainingYes("Some training")
    val positions = Positions(Set(BeneficialOwner, InternalAccountant), startDate)
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
    private val currentAddress = ResponsiblePersonCurrentAddress(newPersonAddress, Some(ZeroToFiveMonths))
    private val additionalAddress = ResponsiblePersonAddress(newAdditionalPersonAddress, Some(ZeroToFiveMonths))

    val personName = PersonName("first", Some("middle"), "last", None, None)
    val contactDetails = ContactDetails("07702743444", "new@test.com")
    val addressHistory = ResponsiblePersonAddressHistory(Some(currentAddress), Some(additionalAddress))
    val personResidenceType = PersonResidenceType(residence, residenceCountry, Some(residenceNationality))
    val saRegistered = SaRegisteredNo
    val vatRegistered = VATRegisteredYes("12345678")
    val positions = Positions(Set(Director, SoleProprietor), startDate)
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
    Some(true),
    false,
    Some(1),
    Some(StatusConstants.Unchanged),
    None,
    Some(DefaultValues.soleProprietorOfAnotherBusiness)
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
      "positions" -> Seq("01", "03"),
      "startDate" -> startDate.get.toString("yyyy-MM-dd")
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
    "soleProprietorOfAnotherBusiness" -> Json.obj(
      "soleProprietorOfAnotherBusiness" -> true
    ),
    "hasAlreadyPassedFitAndProper" -> true,
    "hasChanged" -> false,
    "lineId" -> 1,
    "status" -> "Unchanged"
  )

  /** Make sure Responsible People model is complete */
  assert(CompleteResponsiblePeople.isComplete)

}