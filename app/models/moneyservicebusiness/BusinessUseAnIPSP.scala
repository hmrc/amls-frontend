/*
 * Copyright 2018 HM Revenue & Customs
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

package models.moneyservicebusiness

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import cats.data.Validated.{Invalid, Valid}

sealed trait BusinessUseAnIPSP

case class BusinessUseAnIPSPYes(name: String , reference: String) extends BusinessUseAnIPSP

case object BusinessUseAnIPSPNo extends BusinessUseAnIPSP

object BusinessUseAnIPSP {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  private val maxNameTypeLength = 140
  private val nameType = notEmptyStrip andThen
    notEmpty.withMessage("error.required.msb.ipsp.name") andThen
    maxLength(maxNameTypeLength).withMessage("error.invalid.maxlength.140") andThen
    basicPunctuationPattern()

  private val referenceType = notEmptyStrip andThen
    notEmpty.withMessage("error.invalid.mlr.number") andThen referenceNumberRule()

  implicit val formRule: Rule[UrlFormEncoded, BusinessUseAnIPSP] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "useAnIPSP").read[Boolean].withMessage("error.required.msb.ipsp") flatMap {
      case true =>
        ((__ \ "name").read(nameType) ~
          (__ \ "referenceNumber").read(referenceType)) (BusinessUseAnIPSPYes.apply)
      case false => Rule.fromMapping { _ => Valid(BusinessUseAnIPSPNo) }
    }
  }

  implicit val formWrites: Write[BusinessUseAnIPSP, UrlFormEncoded] = Write {
    case BusinessUseAnIPSPYes(name, number) =>
      Map("useAnIPSP" -> Seq("true"),
        "name" -> Seq(name),
        "referenceNumber" -> Seq(number)
      )
    case BusinessUseAnIPSPNo => Map("useAnIPSP" -> Seq("false"))
  }

  implicit val jsonReads: Reads[BusinessUseAnIPSP] = {
    import play.api.libs.functional.syntax._
    (__ \ "useAnIPSP").read[Boolean] flatMap {
      case true => ((__ \ "name").read[String] and
        (__ \ "referenceNumber").read[String]) (BusinessUseAnIPSPYes.apply _)
      case false => Reads(_ => JsSuccess(BusinessUseAnIPSPNo))
    }
  }

  implicit val jsonWrites = Writes[BusinessUseAnIPSP] {
    case BusinessUseAnIPSPYes(name ,referenceNumber) => Json.obj(
                                          "useAnIPSP" -> true,
                                           "name" -> name,
                                           "referenceNumber" -> referenceNumber

                                        )
    case BusinessUseAnIPSPNo => Json.obj("useAnIPSP" -> false)
  }

}
