package models.estateagentbusiness

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class RedressSchemeSpec extends PlaySpec with MockitoSugar {

  "RedressScheemsSpec" must {

    "validate model with redress option selected as yes" in {

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("01"))) must
        be(Success(ThePropertyOmbudsman))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("02"))) must
        be(Success(OmbudsmanServices))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("03"))) must
        be(Success(PropertyRedressScheme))

      RedressScheme.formRedressRule.validate(Map("isRedress" -> Seq("true"), "propertyRedressScheme" -> Seq("04"), "other" -> Seq("test"))) must
        be(Success(Other("test")))

    }

    "validate model redress option selected as No" in {
      val model = Map(
        "isRedress" -> Seq("false"),
        "propertyRedressScheme" -> Seq("02")
      )

      RedressScheme.formRedressRule.validate(model) must
        be(Success(RedressSchemedNo))
    }

    "fail to validate given an `other` with no value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
          "other" -> Seq("")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Failure(Seq(
          (Path \ "other") -> Seq(ValidationError("error.required.eab.redress.scheme.name"))
        )))
    }

    "fail to validate given an `other` with max value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("04"),
        "other" -> Seq("asadasas"*50)
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Failure(Seq(
          (Path \ "other") -> Seq(ValidationError("error.invalid.eab.redress.scheme.name"))
        )))
    }


    "fail to validate given a non-enum value" in {

      val data = Map(
        "isRedress" -> Seq("true"),
        "propertyRedressScheme" -> Seq("10")
      )

      RedressScheme.formRedressRule.validate(data) must
        be(Failure(Seq(
          (Path \ "propertyRedressScheme") -> Seq(ValidationError("error.invalid"))
        )))
    }

    "write correct data from enum value" in {

      RedressScheme.formRedressWrites.writes(ThePropertyOmbudsman) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("01")))

      RedressScheme.formRedressWrites.writes(OmbudsmanServices) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("02")))

      RedressScheme.formRedressWrites.writes(PropertyRedressScheme) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("03")))

      RedressScheme.formRedressWrites.writes(Other("foobar")) must
        be(Map("isRedress" -> Seq("true"),"propertyRedressScheme" -> Seq("04"), "other" -> Seq("foobar")))

      RedressScheme.formRedressWrites.writes(RedressSchemedNo) must
        be(Map("isRedress" -> Seq("false")))
    }

    "JSON validation" must {
      "successfully validate selecting redress option no" in {

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressSchemedNo, JsPath \ "isRedress"))

      }

      "successfully validate json Reads" in {
        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "01")) must
          be(JsSuccess(ThePropertyOmbudsman, JsPath \ "isRedress" \ "propertyRedressScheme"))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "02")) must
          be(JsSuccess(OmbudsmanServices, JsPath \ "isRedress" \ "propertyRedressScheme"))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> true,"propertyRedressScheme" -> "03")) must
          be(JsSuccess(PropertyRedressScheme, JsPath \ "isRedress" \ "propertyRedressScheme"))

        val json = Json.obj("isRedress"-> true,
                            "propertyRedressScheme" -> "04",
                            "propertyRedressSchemeOther" -> "test")

        Json.fromJson[RedressScheme](json) must
          be(JsSuccess(Other("test"), JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther"))

        Json.fromJson[RedressScheme](Json.obj("isRedress"-> false)) must
          be(JsSuccess(RedressSchemedNo, JsPath \ "isRedress"))

      }

      "fail to validate when given an empty `other` value" in {

        val json = Json.obj("isRedress"-> true,
                             "propertyRedressScheme" -> "04"
                            )

        Json.fromJson[RedressScheme](json) must
          be(JsError((JsPath \ "isRedress" \ "propertyRedressScheme" \ "propertyRedressSchemeOther") -> ValidationError("error.path.missing")))
      }

      "fail to validate when invalid option is passed" in {

        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "10"
        )

        Json.fromJson[RedressScheme](json) must
          be(JsError((JsPath \ "isRedress" \ "propertyRedressScheme") -> ValidationError("error.invalid")))
      }


      "successfully validate json write" in {

        Json.toJson(ThePropertyOmbudsman) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "01"))

        Json.toJson(OmbudsmanServices) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "02"))

        Json.toJson(PropertyRedressScheme) must be(Json.obj("isRedress"-> true, "propertyRedressScheme" -> "03"))

        val json = Json.obj("isRedress"-> true,
          "propertyRedressScheme" -> "04",
          "propertyRedressSchemeOther" -> "test")

        Json.toJson(Other("test")) must be(json)

        Json.toJson(RedressSchemedNo) must be(Json.obj("isRedress"-> false))
      }
    }
  }
}
