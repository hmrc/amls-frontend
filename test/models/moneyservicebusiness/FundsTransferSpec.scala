package models.moneyservicebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import jto.validation._
import jto.validation.forms.UrlFormEncoded

class FundsTransferSpec extends PlaySpec {
  "FundsTransfer" must {

    "Must successfully validate when user selects 'yes' option" in {
      val data: Map[String, Seq[String]] = Map(
      "transferWithoutFormalSystems" -> Seq("true")
      )

      FundsTransfer.formRule.validate(data) must
      be(Valid(FundsTransfer(true)))
    }

    "successfully validate when user selects 'no' option" in {
      val data: UrlFormEncoded = Map(
        "transferWithoutFormalSystems" -> Seq("false")
      )

      FundsTransfer.formRule.validate(data) must
      be(Valid(FundsTransfer(false)))
    }

    "fail validation when data is invalid" in {
      FundsTransfer.formRule.validate(Map.empty) must
        be(Invalid(Seq(
        (Path \ "transferWithoutFormalSystems") -> Seq(ValidationError("error.required.msb.fundsTransfer"))
        )))
    }

    "write correct data" in {

      val model = FundsTransfer(true)

      FundsTransfer.formWrites.writes(model) must
        be(Map(
          "transferWithoutFormalSystems" -> Seq("true")
        ))
    }
  }
}

