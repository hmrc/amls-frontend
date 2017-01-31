package models.tradingpremises

import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import jto.validation.forms.UrlFormEncoded
import jto.validation._
import play.api.libs.json._

class YourTradingPremisesSpec extends WordSpec with MustMatchers {

  val data = Map(
    "tradingName" -> Seq("foo"),
    "addressLine1" -> Seq("1"),
    "addressLine2" -> Seq("2"),
    "postcode" -> Seq("asdfasdf"),
    "isResidential" -> Seq("true"),
    "startDate.day" -> Seq("24"),
    "startDate.month" -> Seq("2"),
    "startDate.year" -> Seq("1990")
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
    new LocalDate(1990, 2, 24)
  )

  "YourTradingPremises" must {

    "fail validation when isresidential not selected" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foo"),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isResidential" -> Seq(""),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990")
      )) must be (Invalid(Seq(Path \ "isResidential"  -> Seq(ValidationError("error.required.tp.residential.address"))
      )))
    }


    "fail vaidation when trading name is exceeds maxlength" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("foooo"*50),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Invalid(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.invalid.tp.trading.name"))
      )))
    }

    "fail vaidation when trading name is empty" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq(""),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Failure(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.required.tp.trading.name"))
      )))
    }

    "fail vaidation when trading name contains only spaces" in {
      YourTradingPremises.formR.validate(Map("tradingName" -> Seq("   "),
        "addressLine1" -> Seq("1"),
        "addressLine2" -> Seq("2"),
        "postcode" -> Seq("asdfasdf"),
        "isResidential" -> Seq("true"),
        "startDate.day" -> Seq("24"),
        "startDate.month" -> Seq("02"),
        "startDate.year" -> Seq("1990"))) must be (Failure(Seq(Path \ "tradingName"  -> Seq(ValidationError("error.required.tp.trading.name"))
      )))
    }

    "Correctly serialise from form data" in {


      implicitly[Rule[UrlFormEncoded, YourTradingPremises]].validate(data) must
        be(Valid(model.copy(tradingNameChangeDate = None)))
    }

    "Correctly write from model to form" in {

      implicitly[Write[YourTradingPremises, UrlFormEncoded]].writes(model.copy(tradingNameChangeDate = None)) must
        be(data)
    }

  }

  "The Json serializer" must {

    val json = Json.obj(
      "tradingName" -> "foo",
      "addressLine1" -> "1",
      "addressLine2" -> "2",
      "addressDateOfChange" -> new LocalDate(1997, 7, 1),
      "postcode" -> "asdfasdf",
      "isResidential" -> true,
      "startDate" -> new LocalDate(1990, 2, 24),
      "tradingNameChangeDate" -> new LocalDate(2016,1,12)
    )

    val jsonModel = model.copy(
      tradingNameChangeDate = Some(DateOfChange(new LocalDate(2016, 1, 12))),
      tradingPremisesAddress = model.tradingPremisesAddress.copy(dateOfChange = Some(DateOfChange(new LocalDate(1997, 7, 1))))
    )

    "Correctly serialise from json" in {
      implicitly[Reads[YourTradingPremises]].reads(json) must
        be(JsSuccess(jsonModel))
    }

    "Correctly write form model to json" in {

      implicitly[Writes[YourTradingPremises]].writes(jsonModel) must
        be(json)
    }
  }
}
