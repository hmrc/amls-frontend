/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait IsKnownByOtherNames

case class IsKnownByOtherNamesYes(otherfirstnames: String, othermiddlenames: Option[String], otherlastnames: String)
    extends IsKnownByOtherNames

case object IsKnownByOtherNamesNo extends IsKnownByOtherNames

object IsKnownByOtherNames {

  implicit val jsonReads: Reads[IsKnownByOtherNames] =
    (__ \ "isKnownByOtherNames").read[Boolean] flatMap {
      case true  =>
        (
          (__ \ "otherfirstnames").read[String] and
            (__ \ "othermiddlenames").readNullable[String] and
            (__ \ "otherlastnames").read[String]
        )(IsKnownByOtherNamesYes.apply _)
      case false => Reads(_ => JsSuccess(IsKnownByOtherNamesNo))
    }

  implicit val jsonWrites: Writes[IsKnownByOtherNames] = Writes[IsKnownByOtherNames] {
    case a: IsKnownByOtherNamesYes =>
      Json.obj(
        "isKnownByOtherNames" -> true,
        "otherfirstnames"     -> a.otherfirstnames,
        "othermiddlenames"    -> a.othermiddlenames,
        "otherlastnames"      -> a.otherlastnames
      )
    case IsKnownByOtherNamesNo     => Json.obj("isKnownByOtherNames" -> false)
  }
}
