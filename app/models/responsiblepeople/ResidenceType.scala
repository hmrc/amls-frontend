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

import models.FormTypes._
import org.joda.time.LocalDate
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, To, Write}
import play.api.libs.json.{Writes, Reads}

sealed trait ResidenceType

case class ResidenceTypeUKResidence(nino: String) extends ResidenceType

case class ResidenceTypeNonUKResidence(
                           dateOfBirth: LocalDate,
                           passportType: PassportType
                         ) extends ResidenceType

object ResidenceType {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ResidenceType] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "isUKResidence").read[Boolean].withMessage("error.required.rp.is.uk.resident") flatMap {
      case true =>
        (__ \ "nino").read(ninoType).map(ResidenceTypeUKResidence.apply)
      case false =>
        (
          (__ \ "dateOfBirth").read(localDateFutureRule) ~
            __.read[PassportType]
          ) (ResidenceTypeNonUKResidence)
    }
  }

  implicit def formWrites
  (implicit
   w: Write[ResidenceTypeNonUKResidence, UrlFormEncoded]
  ) = Write[ResidenceType, UrlFormEncoded] {
    case f: ResidenceTypeUKResidence =>
      Map(
        "isUKResidence" -> Seq("true"),
        "nino" -> Seq(f.nino)
      )
    case f: ResidenceTypeNonUKResidence =>
      Map(
        "isUKResidence" -> Seq("false")
      ) ++
        w.writes(f)

  }

  implicit val formWritesNonUK: Write[ResidenceTypeNonUKResidence, UrlFormEncoded] = To[UrlFormEncoded] { __ =>
    import models.FormTypes.localDateWrite
    import jto.validation.forms.Writes._
    import play.api.libs.functional.syntax.unlift
    (
      (__ \ "dateOfBirth").write(localDateWrite) ~
        __.write[PassportType]
      ) (unlift(ResidenceTypeNonUKResidence.unapply))
  }


  implicit val jsonReads: Reads[ResidenceType] = {
    import play.api.libs.json._
    import play.api.libs.json.Reads._
    import play.api.libs.functional.syntax._
    (__ \ "nino").read[String] map ResidenceTypeUKResidence.apply map identity[ResidenceType] orElse
      (
        (__ \ "dateOfBirth").read[LocalDate] and
          __.read[PassportType]
        ) (ResidenceTypeNonUKResidence.apply _)
  }

  implicit val jsonWrites: Writes[ResidenceType] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    Writes[ResidenceType] {
      case a: ResidenceTypeUKResidence =>
        Json.obj(
          "nino" -> a.nino
        )
      case a: ResidenceTypeNonUKResidence =>
        (
          (__ \ "dateOfBirth").write[LocalDate] and
            __.write[PassportType]
          ) (unlift(ResidenceTypeNonUKResidence.unapply)).writes(a)
    }
  }

}
