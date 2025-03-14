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

package models.declaration

import models.declaration.release7.{RoleType, RoleWithinBusinessRelease7}

case class AddPerson(
  firstName: String,
  middleName: Option[String],
  lastName: String,
  roleWithinBusiness: RoleWithinBusinessRelease7
)

object AddPerson {

  import play.api.libs.json._

  val key = "add-person"

  implicit def convert(role: RoleWithinBusiness): RoleType =
    role match {
      case BeneficialShareholder => models.declaration.release7.BeneficialShareholder
      case Director              => models.declaration.release7.Director
      case Partner               => models.declaration.release7.Partner
      case InternalAccountant    => models.declaration.release7.InternalAccountant
      case ExternalAccountant    => models.declaration.release7.ExternalAccountant
      case SoleProprietor        => models.declaration.release7.SoleProprietor
      case NominatedOfficer      => models.declaration.release7.NominatedOfficer
      case Other(x)              => models.declaration.release7.Other(x)
    }

  implicit val jsonReads: Reads[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Reads._
    import play.api.libs.json._
    (
      (__ \ "firstName").read[String] and
        (__ \ "middleName").readNullable[String] and
        (__ \ "lastName").read[String] and
        __.read[RoleWithinBusinessRelease7]
    )(AddPerson.apply _)

  }

  implicit val jsonWrites: Writes[AddPerson] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json.Writes._
    import play.api.libs.json._
    (
      (__ \ "firstName").write[String] and
        (__ \ "middleName").write[Option[String]] and
        (__ \ "lastName").write[String] and
        __.write[RoleWithinBusinessRelease7]
    )(unlift(AddPerson.unapply))
  }

}
