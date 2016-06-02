package models.moneyservicebusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping._
import play.api.data.mapping.forms.UrlFormEncoded

class FundsTransferSpec extends PlaySpec {
  "FundsTransfer" must {

    "Must successfully validate when user selects 'yes' option" in {
      val data: Map[String, Seq[String]] = Map(
      "transferWithoutFormalSystems" -> Seq("true")
      )

      FundsTransfer.formRule.validate(data) must
      be(Success(FundsTransfer(true)))
    }

    "successfully validate when user selects 'no' option" in {
      val data: UrlFormEncoded = Map(
        "transferWithoutFormalSystems" -> Seq("false")
      )

      FundsTransfer.formRule.validate(data) must
      be(Success(FundsTransfer(false)))
    }

    "fail validation when data is invalid" in {
      FundsTransfer.formRule.validate(Map.empty) must
        be(Failure(Seq(
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

