package models.responsiblepeople

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError

class PersonRegisteredSpec extends PlaySpec with MockitoSugar {

  "PersonRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherPerson" -> Seq("false")
      )

      PersonRegistered.formRule.validate(data) must
        be(Valid(PersonRegistered(false)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "registerAnotherPerson" -> Seq("true")
      )

      PersonRegistered.formRule.validate(data) must
        be(Valid(PersonRegistered(true)))
    }

    "fail to validate when given invalid data" in {

      PersonRegistered.formRule.validate(Map.empty) must
        be(Invalid(Seq(
          (Path \ "registerAnotherPerson") -> Seq(ValidationError("error.required.rp.register.another.person"))
        )))
    }

    "write correct data" in {

      val model = PersonRegistered(true)

      PersonRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherPerson" -> Seq("true")
        ))
    }
  }
}
