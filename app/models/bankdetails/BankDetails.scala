package models.bankdetails

case class BankDetails (
                         bankAccountType: Option[BankAccountType] = None,
                         bankAccountType1: Option[BankAccountType] = None
                        ){

  def bankAccountType(v: BankAccountType): BankDetails =
    this.copy(bankAccountType = Some(v))

  def bankAccountType1(v: BankAccountType): BankDetails =
    this.copy(bankAccountType1 = Some(v))

}

object BankDetails {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  val key = "bank-details"
  implicit val reads: Reads[BankDetails] = (
      __.read[Option[BankAccountType]] and
      __.read[Option[BankAccountType]]
    ) (BankDetails.apply _)

  implicit val writes: Writes[BankDetails] = Writes[BankDetails] {
    model =>
      Seq(
        Json.toJson(model.bankAccountType).asOpt[JsObject],
        Json.toJson(model.bankAccountType1).asOpt[JsObject]
      ).flatten.fold(Json.obj()) {
        _ ++ _
      }
  }

  implicit def default(details: Option[BankDetails]): BankDetails =
    details.getOrElse(BankDetails())
}


