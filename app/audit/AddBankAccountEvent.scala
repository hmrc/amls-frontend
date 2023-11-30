/*
 * Copyright 2023 HM Revenue & Customs
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

package audit

import cats.implicits._
import models.bankdetails._
import play.api.libs.json.{JsString, Writes}
import Utils._
import models.bankdetails.BankAccountType._
import utils.AuditHelper
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent

object AddBankAccountEvent {

  case class BankAccountAuditDetail
  (
    accountName: String,
    bankAccountType: Option[BankAccountType],
    isUKBankAccount: Boolean,
    sortCode: Option[String],
    accountNumber: Option[String],
    iban: Option[String]
  )

  object BankAccountAuditDetail {

    implicit val accountTypeWrites = Writes[BankAccountType] {
      case PersonalAccount => JsString("personal")
      case BelongsToBusiness => JsString("business")
      case BelongsToOtherBusiness => JsString("other business")
      case NoBankAccountUsed => JsString("no bank account")
    }

    implicit val writes: Writes[BankAccountAuditDetail] = {
      import play.api.libs.functional.syntax._
      import play.api.libs.json._
      (
        (__ \ "accountName").write[String] and
          (__ \ "accountType").writeNullable[BankAccountType](accountTypeWrites) and
          (__ \ "isUkBankAccount").write[Boolean] and
          (__ \ "sortCode").writeNullable[String] and
          (__ \ "accountNumber").writeNullable[String] and
          (__ \ "iban").writeNullable[String]
        ) (unlift(BankAccountAuditDetail.unapply))
    }
  }

  implicit def convert(bankDetails: BankDetails): Option[BankAccountAuditDetail] = bankDetails.bankAccount.flatMap  { ba => ((ba.account, bankDetails.accountName) match {
    case (Some(account: UKAccount), Some(name)) =>
        Some(BankAccountAuditDetail(name, bankDetails.bankAccountType, isUKBankAccount = true, account.sortCode.some, account.accountNumber.some, None))

    case (Some(account: NonUKIBANNumber), Some(name)) =>
      Some(BankAccountAuditDetail(name, bankDetails.bankAccountType, isUKBankAccount = false, None, None, account.IBANNumber.some))

    case (Some(account: NonUKAccountNumber), Some(name)) =>
      Some(BankAccountAuditDetail(name, bankDetails.bankAccountType, isUKBankAccount = false, None, account.accountNumber.some, None))

    case _ => None
  })}

  def apply(bankAccount: BankDetails)(implicit hc: HeaderCarrier, request: Request[_]) = DataEvent(
    auditSource = AuditHelper.appName,
    auditType = "manualBankAccountSubmitted",
    tags = hc.toAuditTags("manualBankAccountSubmitted", request.path),
    detail = hc.toAuditDetails() ++ (convert(bankAccount) match {
      case Some(detail) => toMap(detail)
      case _ => Map()
    })
  )

}
