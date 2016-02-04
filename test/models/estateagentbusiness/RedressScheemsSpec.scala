package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RedressScheemsSpec extends PlaySpec with MockitoSugar {

  "RedressScheemsSpec" must {

    "validate model with redress option selected as yes" in {
      val model = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressScheme.formRedressRule.validate(model) must
        be(Success(OmbudsmanServices))
    }

    "validate model redress option selected as No" in {
      val model = Map(
        "isRedress" -> Seq("false"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressScheme.formRedressRule.validate(model) must
        be(Success(RedressSchemedNo))
    }

    "successfully validate given an `other` value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
        "other" -> Seq("foobar")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Success(Other("foobar")))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Failure(Seq(
          (Path \ "other") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      RedressScheme.formRedressWrites.writes(Other("foobar")) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("04"), "other" -> Seq("foobar")))
    }

    "JSON validation" must {
      "successfully validate selecting redress option no" in {

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressSchemedNo, JsPath \ "isRedress"))

      }

      "successfully validate selecting redress option Yes" in {
        val json = Json.obj("isRedress"-> true,
                            "propertyRedressScheme" -> "04",
                            "propertyRedressSchemeOther" -> "test")

        Json.fromJson[RedressScheme](json) must
          be(JsSuccess(Other("test"), JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther"))

      }

      "fail to validate when given an empty `other` value" in {

        val json = Json.obj("isRedress"-> true,
                             "propertyRedressScheme" -> "04"
                            )

        Json.fromJson[RedressScheme](json) must
          be(JsError((JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther") -> ValidationError("error.path.missing")))
      }

      "successfully validate json write" in {
        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> "test")

        Json.toJson(Other("test")) must be(json)

      }
    }

  }
}
