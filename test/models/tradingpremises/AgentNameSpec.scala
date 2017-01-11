package models.tradingpremises

import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.{Failure, Path, Success}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess}

class AgentNameSpec extends PlaySpec {

  "AgentName" must {

    "validate form Read" in {
      val formInput = Map(
        "agentName" -> Seq("sometext"),
        "dateOfChange.day" -> Seq("12"),
        "dateOfChange.month" -> Seq("1"),
        "dateOfChange.year" -> Seq("2016"),
        "activityStartDate" -> Seq(new LocalDate(2016,1,1).toString("yyyy-MM-dd"))
      )

      AgentName.formReads.validate(formInput) must be(Success(AgentName("sometext", Some(DateOfChange(new LocalDate(2016, 1, 12))))))
    }

    "throw error when required field is missing" in {
      val formInput = Map("agentName" -> Seq(""))
      AgentName.formReads.validate(formInput) must be(Failure(Seq((Path \ "agentName", Seq(ValidationError("error.required.tp.agent.name"))))))
    }

    "throw error when input exceeds max length" in {
      val formInput = Map("agentName" -> Seq("sometesttexttest"*11))
      AgentName.formReads.validate(formInput) must be(Failure(Seq((Path \ "agentName") -> Seq(ValidationError("error.invalid.tp.agent.name")))))
    }

    "validate form write" in {
      AgentName.formWrites.writes(AgentName("sometext")) must be(Map("agentName" -> Seq("sometext")))
    }

    "given a dateOfChange before business activity start date" in {

      val data = Map(
        "agentName" -> Seq("sometext"),
        "dateOfChange.day" -> Seq("12"),
        "dateOfChange.month" -> Seq("01"),
        "dateOfChange.year" -> Seq("2016"),
        "activityStartDate" -> Seq("2017-01-01")
      )
      AgentName.formReads.validate(data) must
        be(Failure(Seq(
          (Path \ "dateOfChange") -> Seq(ValidationError("error.expected.regofficedateofchange.date.after.activitystartdate", "01-01-2017"))
        )))
    }

  }

  "Json Validation" must {
    "Successfully read/write Json data" in {
      AgentName.format.reads(AgentName.format.writes(
        AgentName("test", Some(DateOfChange(new LocalDate(2017,1,1)))))) must be(
        JsSuccess(
          AgentName("test", Some(DateOfChange(new LocalDate(2017,1,1))))))
    }

  }
}
