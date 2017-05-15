/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.libs.json._

sealed trait SatisfactionSurvey

object SatisfactionSurvey {

  case class First(details: Option[String]) extends SatisfactionSurvey
  case class Second(details: Option[String]) extends SatisfactionSurvey
  case class Third(details: Option[String]) extends SatisfactionSurvey
  case class Fourth(details: Option[String]) extends SatisfactionSurvey
  case class Fifth(details: Option[String]) extends SatisfactionSurvey

  import utils.MappingUtils.Implicits._

  val maxDetailsLength = 1200
  val detailsRule = maxLength(maxDetailsLength).withMessage("error.invalid.maxlength.1200")

  implicit val formRule: Rule[UrlFormEncoded, SatisfactionSurvey] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "satisfaction").read[String].withMessage("error.survey.satisfaction.required") flatMap {
      case "01" => (__ \ "details").read(optionR(detailsRule)) map First.apply
      case "02" => (__ \ "details").read(optionR(detailsRule)) map Second.apply
      case "03" => (__ \ "details").read(optionR(detailsRule)) map Third.apply
      case "04" => (__ \ "details").read(optionR(detailsRule)) map Fourth.apply
      case "05" => (__ \ "details").read(optionR(detailsRule)) map Fifth.apply
      case _ =>
        (Path \ "satisfaction") -> Seq(ValidationError("error.invalid"))
    }

  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "satisfaction").read[String].flatMap[SatisfactionSurvey] {
      case "01" => (__ \ "details").readNullable[String] map First.apply
      case "02" => (__ \ "details").readNullable[String] map Second.apply
      case "03" => (__ \ "details").readNullable[String] map Third.apply
      case "04" => (__ \ "details").readNullable[String] map Fourth.apply
      case "05" => (__ \ "details").readNullable[String] map Fifth.apply
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }

  implicit val jsonWrites =  Writes[SatisfactionSurvey] {
    case First(details) => Json.obj(
      "satisfaction" -> "01",
      "details" -> details.fold(""){x => x.toString}
    )
    case Second(details) => Json.obj(
      "satisfaction" -> "02",
      "details" -> details.fold(""){x => x.toString}
    )
    case Third(details) => Json.obj(
      "satisfaction" -> "03",
      "details" -> details.fold(""){x => x.toString}
    )
    case Fourth(details) => Json.obj(
      "satisfaction" -> "04",
      "details" -> details.fold(""){x => x.toString}
    )
    case Fifth(details) => Json.obj(
      "satisfaction" -> "05",
      "details" -> details.fold(""){x => x.toString}
    )
  }

}
