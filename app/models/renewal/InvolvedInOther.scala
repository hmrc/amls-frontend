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

package models.renewal

import play.api.libs.json._

sealed trait InvolvedInOther {
  def toBool: Boolean
}

case class InvolvedInOtherYes(details: String) extends InvolvedInOther {
  override def toBool: Boolean = true
}

case object InvolvedInOtherNo extends InvolvedInOther {
  override def toBool: Boolean = false
}

object InvolvedInOther {

  val key = "renewal-involved-in-other"

  def fromBool(bool: Boolean): InvolvedInOther =
    if (bool) {
      InvolvedInOtherYes("")
    } else {
      InvolvedInOtherNo
    }

  implicit val jsonReads: Reads[InvolvedInOther] =
    (__ \ "involvedInOther").read[Boolean] flatMap {
      case true  => (__ \ "details").read[String] map InvolvedInOtherYes.apply
      case false => Reads(_ => JsSuccess(InvolvedInOtherNo))
    }

  implicit val jsonWrites: Writes[InvolvedInOther] = Writes[InvolvedInOther] {
    case InvolvedInOtherYes(details) =>
      Json.obj(
        "involvedInOther" -> true,
        "details"         -> details
      )
    case involvedInOtherNo           => Json.obj("involvedInOther" -> false)
  }

  implicit def convert(model: InvolvedInOther): models.businessactivities.InvolvedInOther = model match {
    case InvolvedInOtherYes(details) => models.businessactivities.InvolvedInOtherYes(details)
    case InvolvedInOtherNo           => models.businessactivities.InvolvedInOtherNo
  }

}
