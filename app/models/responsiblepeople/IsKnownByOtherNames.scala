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

package models.responsiblepeople

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import play.api.libs.json._
import jto.validation.forms.Rules._
import play.api.libs.functional.syntax._
import utils.MappingUtils.Implicits._
import cats.data.Validated.{Invalid, Valid}

sealed trait IsKnownByOtherNames

case class IsKnownByOtherNamesYes(otherfirstnames: String,
                                  othermiddlenames: Option[String],
                                  otherlastnames: String) extends IsKnownByOtherNames

case object IsKnownByOtherNamesNo extends IsKnownByOtherNames

object IsKnownByOtherNames {

  val maxNameTypeLength = 35

  val otherFirstNameType  = notEmpty.withMessage("error.required.otherfirstnames") andThen
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.firstname")

  val otherMiddleNameType  = maxLength(maxNameTypeLength).withMessage("error.invalid.length.middlename")

  val otherLastNameType  = notEmpty.withMessage("error.required.otherlastnames") andThen
    maxLength(maxNameTypeLength).withMessage("error.invalid.length.lastname")

  implicit val formRule: Rule[UrlFormEncoded, IsKnownByOtherNames] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import utils.MappingUtils.Implicits._
    (__ \ "isKnownByOtherNames").read[Boolean].withMessage("error.required.rp.isknownbyothernames") flatMap {
      case true => (
        (__ \ "otherfirstnames").read(otherFirstNameType) ~
        (__ \ "othermiddlenames").read(optionR(otherMiddleNameType)) ~
        (__ \ "otherlastnames").read(otherLastNameType)
        )(IsKnownByOtherNamesYes.apply _)
      case false => Rule.fromMapping { _ => Valid(IsKnownByOtherNamesNo) }
    }
  }

  implicit val formWrites: Write[IsKnownByOtherNames, UrlFormEncoded] = Write {
    case a: IsKnownByOtherNamesYes => Map(
      "isKnownByOtherNames" -> Seq("true"),
      "otherfirstnames" -> Seq(a.otherfirstnames),
      "othermiddlenames" -> Seq(a.othermiddlenames.getOrElse("")),
      "otherlastnames" -> Seq(a.otherlastnames)
    )
    case IsKnownByOtherNamesNo => Map("isKnownByOtherNames" -> Seq("false"))
  }

  implicit val jsonReads: Reads[IsKnownByOtherNames] =
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true => (
        (__ \ "otherfirstnames").read[String] and
          (__ \ "othermiddlenames").readNullable[String] and
          (__ \ "otherlastnames").read[String]
        ) (IsKnownByOtherNamesYes.apply _)
      case false => Reads(_ => JsSuccess(IsKnownByOtherNamesNo))
    }

  implicit val jsonWrites = Writes[IsKnownByOtherNames] {
    case a : IsKnownByOtherNamesYes => Json.obj(
      "isKnownByOtherNames" -> true,
      "otherfirstnames" -> a.otherfirstnames,
      "othermiddlenames" -> a.othermiddlenames,
      "otherlastnames" -> a.otherlastnames
    )
    case IsKnownByOtherNamesNo => Json.obj("isKnownByOtherNames" -> false)
  }

}


