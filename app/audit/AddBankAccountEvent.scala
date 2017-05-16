package audit

import models.bankdetails._
import play.api.libs.json.{JsObject, JsString, Json, Writes}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions._

object AddBankAccountEvent {

  implicit val bankAccountWrites = Writes[BankDetails] { model =>
    Json.obj(
      "accountName" -> model.bankAccount.fold("")(_.accountName),
      "accountType" -> model.bankAccountType.fold("") {
        case PersonalAccount => "personal"
        case BelongsToBusiness => "business"
        case BelongsToOtherBusiness => "other business"
      },
      "isUkBankAccount" -> model.bankAccount.fold(false)(_.account match {
        case UKAccount(_,_) => true
        case _ => false
      }),
      "sortCode" -> model.bankAccount.fold("")(_.account match {
        case UKAccount(_, sc) => sc
        case _ => ""
      }),
      "accountNumber" -> model.bankAccount.fold("")(_.account match {
        case UKAccount(ac, _) => ac
        case _ => ""
      }),
      "iban" -> model.bankAccount.fold("")(_.account match {
        case NonUKIBANNumber(num) => num
        case _ => ""
      })
    )
  }

  private def toMap[A](model: A)(implicit writes: Writes[A]) = Json.toJson(model).as[JsObject].value.mapValues {
    case JsString(v) => v // for some reason, if you don't do this, it puts two double quotes around the resulting string
    case v => v.toString
  }

  def apply(bankAccount: BankDetails)(implicit hc: HeaderCarrier): DataEvent = DataEvent(
    auditSource = AppName.appName,
    auditType = "manualBankAccountSubmitted",
    tags = hc.toAuditTags("manualBankAccountSubmitted", "n/a"),
    detail = hc.toAuditDetails() ++ toMap(bankAccount)
  )

}
