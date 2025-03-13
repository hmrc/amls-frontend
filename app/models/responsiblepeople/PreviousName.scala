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

import play.api.libs.json.{Json, OWrites, Reads}

case class PreviousName(
  hasPreviousName: Option[Boolean] = None,
  firstName: Option[String],
  middleName: Option[String],
  lastName: Option[String]
) {

  val fullName: String = Seq(firstName, middleName, lastName).flatten[String].mkString(" ")
}

object PreviousName {

  implicit val jsonReads: Reads[PreviousName] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._

    (__ \ "hasPreviousName").readNullable[Boolean] and
      (__ \ "firstName").readNullable[String] and
      (__ \ "middleName").readNullable[String] and
      (__ \ "lastName").readNullable[String]
  }.apply(PreviousName.apply _)

  implicit val jsonWrites: OWrites[PreviousName] = Json.writes[PreviousName]
}
