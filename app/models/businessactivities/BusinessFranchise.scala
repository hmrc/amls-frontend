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

package models.businessactivities

import jto.validation._
import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._

sealed trait BusinessFranchise

case class BusinessFranchiseYes(value: String) extends BusinessFranchise

case object BusinessFranchiseNo extends BusinessFranchise

object BusinessFranchise {

  import models.FormTypes._
  import utils.MappingUtils.Implicits._

  private val maxFranchiseName = 140
  private val franchiseNameType =  notEmptyStrip andThen notEmpty.withMessage("error.required.ba.franchise.name") andThen
    maxLength(maxFranchiseName).withMessage("error.max.length.ba.franchise.name") andThen basicPunctuationPattern()

  implicit val formRule: Rule[UrlFormEncoded, BusinessFranchise] = From[UrlFormEncoded] { __ =>
  import jto.validation.forms.Rules._
    (__ \ "businessFranchise").read[Boolean].withMessage("error.required.ba.is.your.franchise") flatMap {
      case true =>
        (__ \ "franchiseName").read(franchiseNameType) map BusinessFranchiseYes.apply
      case false => Rule.fromMapping { _ => Valid(BusinessFranchiseNo) }
    }
  }

  implicit val formWrites: Write[BusinessFranchise, UrlFormEncoded] = Write {
    case BusinessFranchiseYes(value) =>
      Map("businessFranchise" -> Seq("true"),
          "franchiseName" -> Seq(value)
      )
    case BusinessFranchiseNo => Map("businessFranchise" -> Seq("false"))
  }

  implicit val jsonReads: Reads[BusinessFranchise] =
    (__ \ "businessFranchise").read[Boolean] flatMap {
      case true => (__ \ "franchiseName").read[String] map BusinessFranchiseYes.apply
      case false => Reads(_ => JsSuccess(BusinessFranchiseNo))
    }

  implicit val jsonWrites = Writes[BusinessFranchise] {
    case BusinessFranchiseYes(value) => Json.obj(
      "businessFranchise" -> true,
      "franchiseName" -> value
    )
    case BusinessFranchiseNo => Json.obj("businessFranchise" -> false)
  }

}

