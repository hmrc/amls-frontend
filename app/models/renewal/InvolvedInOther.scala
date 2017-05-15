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

package models.renewal

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait InvolvedInOther

case class InvolvedInOtherYes(details: String) extends InvolvedInOther

case object InvolvedInOtherNo extends InvolvedInOther

object InvolvedInOther {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  val key = "renewal-involved-in-other"

  val maxOtherBusinessActivityTypeLength = 255
  val OtherBusinessActivityType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.renewal.ba.involved.in.other.text") andThen
    maxWithMsg(maxOtherBusinessActivityTypeLength, "error.invalid.maxlength.255") andThen
    basicPunctuationPattern()


  implicit val formRule: Rule[UrlFormEncoded, InvolvedInOther] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "involvedInOther").read[Boolean].withMessage("error.required.renewal.ba.involved.in.other") flatMap {
      case true =>
        (__ \ "details").read(OtherBusinessActivityType) map InvolvedInOtherYes.apply
      case false => Rule.fromMapping { _ => Valid(InvolvedInOtherNo) }
    }
  }

  implicit val formWrites: Write[InvolvedInOther, UrlFormEncoded] = Write {
    case InvolvedInOtherYes(value) =>
      Map("involvedInOther" -> Seq("true"),
        "details" -> Seq(value)
      )
    case involvedInOtherNo => Map("involvedInOther" -> Seq("false"))
  }

  implicit val jsonReads: Reads[InvolvedInOther] =
    (__ \ "involvedInOther").read[Boolean] flatMap {
      case true => (__ \ "details").read[String] map InvolvedInOtherYes.apply
      case false => Reads(_ => JsSuccess(InvolvedInOtherNo))
    }

  implicit val jsonWrites = Writes[InvolvedInOther] {
    case InvolvedInOtherYes(details) => Json.obj(
      "involvedInOther" -> true,
      "details" -> details
    )
    case involvedInOtherNo => Json.obj("involvedInOther" -> false)
  }

  implicit def convert(model: InvolvedInOther): models.businessactivities.InvolvedInOther = model match {
    case InvolvedInOtherYes(details) => models.businessactivities.InvolvedInOtherYes(details)
    case InvolvedInOtherNo => models.businessactivities.InvolvedInOtherNo
  }
  
}
