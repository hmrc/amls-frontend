package models.bankdetails

import models.responsiblepeople.BankAccountRegistered
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation.{Failure, Path, Success}
import jto.validation.ValidationError

class BankAccountRegisteredSpec extends PlaySpec with MockitoSugar {

  "BankAccountRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherBank" -> Seq("true")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Success(BankAccountRegistered(true)))
    }

    "successfully validate given a data model containing true" in {

      val data = Map(
        "registerAnotherBank" -> Seq("true")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Success(BankAccountRegistered(true)))
    }

    "successfully validate given a data model containing false" in {

      val data = Map(
        "registerAnotherBank" -> Seq("false")
      )

      BankAccountRegistered.formRule.validate(data) must
        be(Success(BankAccountRegistered(false)))
    }

    "fail to validate when given invalid data" in {

      BankAccountRegistered.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "registerAnotherBank") -> Seq(ValidationError("error.required.bankdetails.register.another.bank"))
        )))
    }

    "write correct data" in {

      val model = BankAccountRegistered(true)

      BankAccountRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherBank" -> Seq("true")
        ))
    }
  }
}
