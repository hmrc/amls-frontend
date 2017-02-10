package models.tradingpremises

import jto.validation.{Invalid, Path, Valid, ValidationError}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json


class AgentRemovalReasonSpec extends PlaySpec {

  "The AgentRemovalReason model" when {

    "given a valid form" must {

      "return the model" when {

        "given 'Other' as a reason" in {

          val form = Map(
            "removalReason" -> Seq("Other"),
            "removalReasonOther" -> Seq("Some other reason")
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Valid(AgentRemovalReason("Other", Some("Some other reason"))))

        }

        "something other than 'Other' was given as the reason" in {

          val form = Map(
            "removalReason" -> Seq("Serious compliance failures")
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Valid(AgentRemovalReason("Serious compliance failures")))

        }
      }
    }

    "given an invalid form" must {

      "fail validation" when {

        "there is no removal reason specified" in {

          val form = Map.empty[String, Seq[String]]

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq((Path \ "removalReason", Seq(ValidationError("error.required"))))))

        }

        "there is no other removal reason when 'Other' is selected" in {

          val form = Map("removalReason" -> Seq("Other"))

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq(Path \ "removalReasonOther" -> Seq(ValidationError("error.required")))))

        }

        "the given reason for 'Other' is too long" in {

          val form = Map(
            "removalReason" -> Seq("Other"),
            "removalReasonOther" -> Seq("a" * 300)
          )

          val result = AgentRemovalReason.formReader.validate(form)

          result must be(Invalid(Seq(Path \ "removalReasonOther" -> Seq(ValidationError("tradingpremises.remove_reasons.agent.other.too_long")))))

        }

      }

    }

    "given a valid model" must {

      "return the form" in {

        val model = AgentRemovalReason("Other", Some("Some other reason"))

        val result = AgentRemovalReason.formWriter.writes(model)

        result must be(Map(
          "removalReason" -> Seq("Other"),
          "removalReasonOther" -> Seq("Some other reason")
        ))

      }

      "return the correct json" in {
        val model = AgentRemovalReason("Other", Some("Some other reason"))

        val json = Json.toJson(model) must be(Json.obj(
          "removalReason" -> "Other",
          "removalReasonOther" -> "Some other reason"
        ))
      }

    }

    "given valid json" must {

      val json =
        """
          | {
          | "removalReason": "Other",
          | "removalReasonOther": "Some reason"
          | }
        """.stripMargin

      "return the model" in {

        Json.parse(json).asOpt[AgentRemovalReason] must be(Some(AgentRemovalReason("Other", Some("Some reason"))))

      }

    }

  }

}
