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

package models.notifications

import play.api.libs.json._

sealed trait StatusType

object StatusType {

  case object Approved extends StatusType
  case object Rejected extends StatusType
  case object Revoked extends StatusType
  case object DeRegistered extends StatusType
  case object Expired extends StatusType

  implicit val jsonReads: Reads[StatusType] =
    Reads {
      case JsString("04") => JsSuccess(Approved)
      case JsString("06") => JsSuccess(Rejected)
      case JsString("08") => JsSuccess(Revoked)
      case JsString("10") => JsSuccess(DeRegistered)
      case JsString("11") => JsSuccess(Expired)
      case _ => JsError(JsPath -> play.api.data.validation.ValidationError("error.invalid"))
    }

  implicit val jsonWrites =
    Writes[StatusType] {
      case Approved => JsString("04")
      case Rejected => JsString("06")
      case Revoked => JsString("08")
      case DeRegistered => JsString("10")
      case Expired => JsString("11")
    }
}
