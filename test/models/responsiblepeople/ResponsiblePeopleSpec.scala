package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class ResponsiblePeopleSpec extends PlaySpec with MockitoSugar {

  val addPerson = AddPerson("John", Some("Envy"), "Doe", false)
  val responsiblePeopleModel = ResponsiblePeople(Some(addPerson))

  "ResponsiblePeople" must {

    "update the model with the person" in {
      val addPersonUpdated = addPerson.copy(firstName = "Johny")
      val newResponsiblePeople = responsiblePeopleModel.addPerson(addPersonUpdated)
      newResponsiblePeople.addPerson.get.firstName must be(addPersonUpdated.firstName)
    }

    "validate complete json" must {

      val completeJson = Json.obj(
        "addPerson" -> Json.obj(
          "firstName" -> "John",
          "middleName" -> "Envy",
          "lastName" -> "Doe",
          "isKnownByOtherNames" -> "false"
        ))

      "Serialise as expected" in {
        Json.toJson(responsiblePeopleModel) must be(completeJson)
      }

      "Deserialise as expected" in {
        completeJson.as[ResponsiblePeople] must be(responsiblePeopleModel)
      }

    }

    "implicitly return an existing Model if one present" in {
      val responsiblePeople = ResponsiblePeople.default(Some(responsiblePeopleModel))
      responsiblePeople must be(responsiblePeopleModel)
    }

    "implicitly return an empty Model if not present" in {
      val responsiblePeople = ResponsiblePeople.default(None)
      responsiblePeople must be(ResponsiblePeople())
    }

  }

}
