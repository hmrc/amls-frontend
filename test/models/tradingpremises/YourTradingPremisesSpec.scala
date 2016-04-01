package models.tradingpremises

import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.mapping.forms.UrlFormEncoded
import play.api.data.mapping._
import play.api.libs.json._

class YourTradingPremisesSpec extends WordSpec with MustMatchers {

  "YourTradingPremises" must {

    val data = Map(
      "tradingName" -> Seq("foo"),
      "addressLine1" -> Seq("1"),
      "addressLine2" -> Seq("2"),
      "postcode" -> Seq("asdfasdf"),
      "isOwner" -> Seq("true"),
      "startDate.day" -> Seq("24"),
      "startDate.month" -> Seq("2"),
      "startDate.year" -> Seq("1990"),
      "isResidential" -> Seq("true")
    )

    val json = Json.obj(
      "tradingName" -> "foo",
      "addressLine1" -> "1",
      "addressLine2" -> "2",
      "postcode" -> "asdfasdf",
      "isOwner" -> true,
      "startDate" -> new LocalDate(1990, 2, 24),
      "isResidential" -> true
    )

    val model = YourTradingPremises(
      "foo",
      Address(
        "1",
        "2",
        None,
        None,
        "asdfasdf"
      ),
      true,
      new LocalDate(1990, 2, 24),
      true
    )

    "fail vaidation when isOwner and isresidential not selected" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foo"),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isOwner" -> Seq(""),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"),
        "isResidential" -> Seq(""))) must be (Failure(Seq(Path \ "isOwner"  -> Seq(ValidationError("error.required.tp.your.business.or.other")),
        Path \ "isResidential"  -> Seq(ValidationError("error.required.tp.residential.address"))
      )))
    }


    "fail vaidation when trading name is exceeds maxlength" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foooo"*50),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isOwner" -> Seq("false"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"),
        "isResidential" -> Seq("true"))) must be (Failure(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.invalid.tp.trading.name"))
      )))
    }

    "Correctly serialise from form data" in {

      implicitly[Rule[UrlFormEncoded, YourTradingPremises]].validate(data) must
        be(Success(model))
    }

    "Correctly write from model to form" in {

      implicitly[Write[YourTradingPremises, UrlFormEncoded]].writes(model) must
        be(data)
    }

    "Correctly serialise from json" in {

      implicitly[Reads[YourTradingPremises]].reads(json) must
        be(JsSuccess(model))
    }

    "Correctly write form model to json" in {

      implicitly[Writes[YourTradingPremises]].writes(model) must
        be(json)
    }
  }
}
