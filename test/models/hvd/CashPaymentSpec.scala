package models.hvd

import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}

class CashPaymentSpec extends PlaySpec with MockitoSugar {

  "CashPaymentSpec" should {
    // scalastyle:off
    val DefaultCashPaymentYes = CashPaymentYes(new LocalDate(1990, 2, 24))

    "Form Validation" must {

      "successfully validate given an enum value" in {

        CashPayment.formRule.validate(Map("acceptedAnyPayment" -> Seq("false"))) must
          be(Success(CashPaymentNo))
      }

      "successfully validate given an `Yes` value" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq("15"),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Success(CashPaymentYes(new LocalDate(1956, 2, 15))))
      }


      "fail to validate given missing mandatory field" in {

        CashPayment.formRule.validate(Map.empty) must
          be(Failure(Seq(
            (Path \ "acceptedAnyPayment") -> Seq(ValidationError("error.required.hvd.accepted.cash.payment"))
          )))
      }

      "fail to validate given an `Yes` with no value" in {

        val data = Map(
          "acceptedAnyPayment" -> Seq("true"),
          "paymentDate.day" -> Seq(""),
          "paymentDate.month" -> Seq("2"),
          "paymentDate.year" -> Seq("1956")
        )

        CashPayment.formRule.validate(data) must
          be(Failure(Seq(Path \ "paymentDate" -> Seq(
            ValidationError("error.expected.jodadate.format", "yyyy-MM-dd")))))
      }

      "write correct data from enum value" in {

        CashPayment.formWrites.writes(CashPaymentNo) must
          be(Map("acceptedAnyPayment" -> Seq("false")))

      }

      "write correct data from `Yes` value" in {

        CashPayment.formWrites.writes(DefaultCashPaymentYes) must
          be(Map("acceptedAnyPayment" -> Seq("true"),
            "paymentDate.day" -> List("24"), "paymentDate.month" -> List("2"), "paymentDate.year" -> List("1990")))
      }
    }

    "JSON validation" must {

      "successfully validate given an enum value" in {

        Json.fromJson[CashPayment](Json.obj("acceptedAnyPayment" -> false)) must
          be(JsSuccess(CashPaymentNo, JsPath \ "acceptedAnyPayment"))
      }

      "successfully validate given an `Yes` value" in {

        val json = Json.obj("acceptedAnyPayment" -> true, "paymentDate" ->"1990-02-24")

        Json.fromJson[CashPayment](json) must
          be(JsSuccess(CashPaymentYes(new LocalDate(1990, 2, 24)), JsPath \ "acceptedAnyPayment" \ "paymentDate"))
      }

      "fail to validate when given an empty `Yes` value" in {

        val json = Json.obj("acceptedAnyPayment" -> true)

        Json.fromJson[CashPayment](json) must
          be(JsError((JsPath \ "acceptedAnyPayment" \ "paymentDate") -> ValidationError("error.path.missing")))
      }

      "Successfully read and write Json data" in {

        CashPayment.jsonReads.reads(CashPayment.jsonWrites.writes(DefaultCashPaymentYes)) must be(
          JsSuccess(CashPaymentYes(new LocalDate(1990, 2, 24)), JsPath \ "acceptedAnyPayment" \ "paymentDate"))
      }

      "write the correct value" in {

        Json.toJson(CashPaymentNo) must
          be(Json.obj("acceptedAnyPayment" -> false))

        Json.toJson(DefaultCashPaymentYes) must
          be(Json.obj(
            "acceptedAnyPayment" -> true,
            "paymentDate" -> new LocalDate(1990, 2, 24)
          ))
      }
    }
  }

}
