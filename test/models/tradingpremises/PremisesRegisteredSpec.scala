package models.tradingpremises

import models.responsiblepeople.PremisesRegistered
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError

class PremisesRegisteredSpec extends PlaySpec with MockitoSugar {

  "PremisesRegistered" must {

    "validate the given model" in {
      val data = Map(
        "registerAnotherPremises" -> Seq("true")
      )

      PremisesRegistered.formRule.validate(data) must
        be(Success(PremisesRegistered(true)))
    }

    "successfully validate given a data model" in {

      val data = Map(
        "registerAnotherPremises" -> Seq("true")
      )

      PremisesRegistered.formRule.validate(data) must
        be(Success(PremisesRegistered(true)))
    }

    "fail to validate when given invalid data" in {

      PremisesRegistered.formRule.validate(Map.empty) must
        be(Failure(Seq(
          (Path \ "registerAnotherPremises") -> Seq(ValidationError("error.required.tp.register.another.premises"))
        )))
    }

    "write correct data" in {

      val model = PremisesRegistered(true)

      PremisesRegistered.formWrites.writes(model) must
        be(Map(
          "registerAnotherPremises" -> Seq("true")
        ))
    }
  }
}
