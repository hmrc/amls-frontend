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

package models.responsiblepeople

import jto.validation._
import jto.validation.forms.Rules._
import play.api.libs.json.{Writes => _}
import utils.MappingUtils.Implicits._
import jto.validation.forms.UrlFormEncoded
import cats.data.Validated.{Invalid, Valid}
import models.FormTypes._

sealed trait Training

case class TrainingYes(information: String) extends Training

case object TrainingNo extends Training

object Training {

  import play.api.libs.json._

  val maxInformationTypeLength = 255
  val informationType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.rp.training.information") andThen
    maxLength(maxInformationTypeLength).withMessage("error.invalid.maxlength.255") andThen
    basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, Training] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "training").read[Boolean].withMessage("error.required.rp.training") flatMap {
      case true =>
        (__ \ "information").read(informationType) map (TrainingYes.apply)
      case false => Rule.fromMapping { _ => Valid(TrainingNo) }
    }
  }

  implicit val formWrites: Write[Training, UrlFormEncoded] = Write {
    case a: TrainingYes => Map(
      "training" -> Seq("true"),
      "information" -> Seq(a.information)
    )
    case TrainingNo => Map("training" -> Seq("false"))
  }

  implicit val jsonReads: Reads[Training] =
    (__ \ "training").read[Boolean] flatMap {
      case true => (__ \ "information").read[String] map (TrainingYes.apply _)
      case false => Reads(_ => JsSuccess(TrainingNo))
    }

  implicit val jsonWrites = Writes[Training] {
    case TrainingYes(information) => Json.obj(
      "training" -> true,
      "information" -> information
    )
    case TrainingNo => Json.obj("training" -> false)
  }

}
