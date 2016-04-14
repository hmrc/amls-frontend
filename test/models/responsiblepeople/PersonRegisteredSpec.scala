package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class PersonRegisteredSpec extends PlaySpec with MockitoSugar {

  "PersonRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnother" -> Seq("true")
      )

      PersonRegistered.formRule.validate(data) must
        be(Success(PersonRegistered(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "registerAnother" -> Seq("true")
      )

      PersonRegistered.formRule.validate(data) must
        be(Success(PersonRegistered(true)))
    }

    "fail to validate when given invalid data" in {

      PersonRegistered.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "registerAnother") -> Seq(ValidationError("error.required.rp.register.another.person"))
        )))
    }

    "write correct data" in {

      val model = PersonRegistered(true)

      PersonRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnother" -> Seq("true")
        ))
    }
  }
}
