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

package models.bankdetails

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}

sealed trait BankAccountType {

  val value: String
  def getBankAccountTypeID: String = {

    import models.bankdetails.BankAccountType._

    this match {
      case a @ PersonalAccount        => a.value
      case a @ BelongsToBusiness      => a.value
      case a @ BelongsToOtherBusiness => a.value
      case a @ NoBankAccountUsed      => a.value
    }
  }
}

object BankAccountType extends Enumerable.Implicits {

  case object PersonalAccount extends WithName("personalAccount") with BankAccountType {
    override val value: String = "01"
  }

  case object BelongsToBusiness extends WithName("belongsToBusiness") with BankAccountType {
    override val value: String = "02"
  }

  case object BelongsToOtherBusiness extends WithName("belongsToOtherBusiness") with BankAccountType {
    override val value: String = "03"
  }

  case object NoBankAccountUsed extends WithName("noBankAccountUsed") with BankAccountType {
    override val value: String = "04"
  }

  def formItems(implicit messages: Messages): Seq[RadioItem] =
    Seq(BelongsToOtherBusiness, BelongsToBusiness, PersonalAccount) map { obj =>
      RadioItem(
        content = Text(messages(s"bankdetails.accounttype.lbl.${obj.value}")),
        id = Some(obj.toString),
        value = Some(obj.toString)
      )
    }

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[BankAccountType] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "bankAccountType").read[String] flatMap {
      case "01" => PersonalAccount
      case "02" => BelongsToBusiness
      case "03" => BelongsToOtherBusiness
      case "04" => NoBankAccountUsed
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[BankAccountType] = Writes[BankAccountType] {
    case PersonalAccount        => Json.obj("bankAccountType" -> "01")
    case BelongsToBusiness      => Json.obj("bankAccountType" -> "02")
    case BelongsToOtherBusiness => Json.obj("bankAccountType" -> "03")
    case NoBankAccountUsed      => Json.obj("bankAccountType" -> "04")
  }

  val all = Seq(
    PersonalAccount,
    BelongsToBusiness,
    BelongsToOtherBusiness,
    NoBankAccountUsed
  )

  implicit val enumerable: Enumerable[BankAccountType] = Enumerable(all.map(v => v.toString -> v): _*)
}
