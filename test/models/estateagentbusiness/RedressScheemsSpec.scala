package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RedressScheemsSpec extends PlaySpec with MockitoSugar {

  "RedressScheemsSpec" must {

    "validate model with few check box selected" in {
      val model = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressRegistered.formRule.validate(model) must
        be(Success(RedressRegisteredYes(OmbudsmanServices)))
    }

    "successfully validate given an `other` value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
        "other" -> Seq("foobar")
      )

      RedressRegistered.formRule.validate(data) must
        be(Success(RedressRegisteredYes(Other("foobar"))))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04")
      )

      RedressRegistered.formRule.validate(data) must
        be(Failure(Seq(
          (Path \ "other") -> Seq(ValidationError("error.required"))
        )))
    }

    "write correct data from enum value" in {

      RedressRegistered.formWrites.writes(RedressRegisteredYes(Other("foobar"))) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("04"), "other" -> Seq("foobar")))
    }

    "JSON validation" must {
      "successfully validate selecting redress option no" in {

        Json.fromJson[RedressRegistered](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressRegisteredNo, JsPath \ "isRedress"))

      }

      "successfully validate selecting redress option Yes" in {
        val json = Json.obj("isRedress"-> true,
                            "propertyRedressScheme" -> "04",
                            "propertyRedressSchemeOther" -> "test")

        Json.fromJson[RedressRegistered](json) must
          be(JsSuccess(RedressRegisteredYes(Other("test")), JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther"))

      }

      "fail to validate when given an empty `other` value" in {

        val json = Json.obj("isRedress"-> true,
                             "propertyRedressScheme" -> "04"
                            )

        Json.fromJson[RedressRegistered](json) must
          be(JsError((JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther") -> ValidationError("error.path.missing")))
      }

      "successfully validate json write" in {
        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> "test")

        Json.toJson(RedressRegisteredYes(Other("test"))) must
          be(json)

      }
    }

  }
}
