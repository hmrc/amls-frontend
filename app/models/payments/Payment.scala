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

package models.payments

import enumeratum.{Enum, EnumEntry}
import models.EnumFormat
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.custom.JsPathSupport.RichJsPath

import java.time.LocalDateTime

sealed abstract class PaymentStatus(val isFinal: Boolean, val validNextStates: Seq[PaymentStatus] = Seq())
    extends EnumEntry

object PaymentStatuses extends Enum[PaymentStatus] {
  case object Created extends PaymentStatus(false, Seq(Sent))
  case object Successful extends PaymentStatus(true)
  case object Sent extends PaymentStatus(false, Seq(Successful, Failed, Cancelled))
  case object Failed extends PaymentStatus(true)
  case object Cancelled extends PaymentStatus(true)

  override def values = findValues
}

case class Payment(
  _id: String,
  amlsRefNo: String,
  safeId: String,
  reference: String,
  description: Option[String],
  amountInPence: Int,
  status: PaymentStatus,
  createdAt: LocalDateTime,
  isBacs: Option[Boolean] = None,
  updatedAt: Option[LocalDateTime] = None
)

object Payment {
  implicit val statusFormat: Format[PaymentStatus] = EnumFormat(PaymentStatuses)

  implicit val writes: OWrites[Payment] = Json.writes[Payment]

  implicit val reads: Reads[Payment] =
    (
      (__ \ "_id").read[String] and
        (__ \ "amlsRefNo").read[String] and
        (__ \ "safeId").read[String] and
        (__ \ "reference").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "amountInPence").read[Int] and
        (__ \ "status").read[PaymentStatus] and
        (__ \ "createdAt").readLocalDateTime and
        (__ \ "isBacs").readNullable[Boolean] and
        (__ \ "updatedAt").readNullable[LocalDateTime]
    )(Payment(_, _, _, _, _, _, _, _, _, _))

  implicit val format: OFormat[Payment] = OFormat(reads, writes)
}
