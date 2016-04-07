package models.responsiblepeople

import models.Country
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.cache.client.CacheMap

class ResponsiblePeopleSpec extends PlaySpec with MockitoSugar {

  val DefaultAddPerson = AddPerson("John", Some("Envy"), "Doe", IsKnownByOtherNamesNo)
  val DefaultPersonResidenceType = PersonResidenceType(UKResidence("AA3464646"), Country("United Kingdom", "GB"), Country("United Kingdom", "GB"))
  val DefaultSaRegisteredYes = SaRegisteredYes("0123456789")

  val NewAddPerson = AddPerson("first", Some("middle"), "last", IsKnownByOtherNamesNo)
  val NewPersonResidenceType = PersonResidenceType(NonUKResidence(new LocalDate(1990, 2, 24), UKPassport("123464646")),
    Country("United Kingdom", "GB"), Country("United Kingdom", "GB"))
  val NewSaRegisteredYes = SaRegisteredNo

  val ResponsiblePeopleModel = ResponsiblePeople(Some(DefaultAddPerson))

  "ResponsiblePeople" must {

    "update the model with the person" in {
      val addPersonUpdated = DefaultAddPerson.copy(firstName = "Johny")
      val newResponsiblePeople = ResponsiblePeopleModel.addPerson(addPersonUpdated)
      newResponsiblePeople.addPerson.get.firstName must be(addPersonUpdated.firstName)
    }

    "validate complete json" must {

      val completeJson = Json.obj(
        "addPerson" -> Json.obj(
          "firstName" -> "John",
          "middleName" -> "Envy",
          "lastName" -> "Doe",
          "isKnownByOtherNames" -> false
        ))

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

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.saRegistered(DefaultSaRegisteredYes)
        result must be (ResponsiblePeople(None, None, Some(DefaultSaRegisteredYes)))
      }
    }
  }

  "Merge with existing model" when {
    val initial = ResponsiblePeople(Some(DefaultAddPerson), Some(DefaultPersonResidenceType), Some(DefaultSaRegisteredYes))

    "Merged with add person" must {
      "return ResponsiblePeople with correct add person" in {
        val result = initial.addPerson(NewAddPerson)
        result must be (ResponsiblePeople(Some(NewAddPerson), Some(DefaultPersonResidenceType), Some(DefaultSaRegisteredYes)))
      }
    }

    "Merged with DefaultPersonResidenceType" must {
      "return ResponsiblePeople with correct DefaultPersonResidenceType" in {
        val result = initial.personResidenceType(NewPersonResidenceType)
        result must be (ResponsiblePeople(Some(DefaultAddPerson), Some(NewPersonResidenceType), Some(DefaultSaRegisteredYes)))
      }
    }

    "Merged with DefaultSaRegisteredYes" must {
      "return ResponsiblePeople with correct DefaultSaRegisteredYes" in {
        val result = initial.saRegistered(NewSaRegisteredYes)
        result must be (ResponsiblePeople(Some(DefaultAddPerson), Some(DefaultPersonResidenceType), Some(NewSaRegisteredYes)))
      }
    }
  }

  "section" must {
    "load what you need page" in {
      val cacheMap = CacheMap("", Map.empty)

      when(cache.getEntry[Seq[ResponsiblePeople]](key)(any())(any(), any(), any()))
        .thenReturn(Future.successful(None))


    }
  }
}
