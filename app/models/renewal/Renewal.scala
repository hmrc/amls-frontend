package models.renewal

import play.api.libs.json.Json

case class Renewal
(
  involvedInOtherActivities: Option[InvolvedInOther] = None,
  businessTurnover: Option[BusinessTurnover] = None,
  turnover: Option[AMLSTurnover] = None,
  customersOutsideUK: Option[CustomersOutsideUK] = None,
  percentageOfCashPaymentOver15000: Option[PercentageOfCashPaymentOver15000] = None,
  msbThroughput: Option[MsbThroughput] = None,
  msbWhichCurrencies: Option[MsbWhichCurrencies] = None,
  hasChanged: Boolean = false
)
{
  def isComplete = {
    this match {
      case Renewal(Some(InvolvedInOtherYes(_)), Some(_), Some(_), Some(_), _, _,_, _) => true
      case Renewal(Some(InvolvedInOtherNo), None, Some(_), Some(_), _, _,_ , _) => true
      case _ => false
    }
  }

  def involvedInOtherActivities(model: InvolvedInOther): Renewal =
    this.copy(involvedInOtherActivities = Some(model), hasChanged = hasChanged || !this.involvedInOtherActivities.contains(model))

  def businessTurnover(model: BusinessTurnover): Renewal =
    this.copy(businessTurnover = Some(model), hasChanged = hasChanged || !this.businessTurnover.contains(model))

  def turnover(model: AMLSTurnover): Renewal =
    this.copy(turnover = Some(model), hasChanged = hasChanged || !this.turnover.contains(model))

  def customersOutsideUK(model: CustomersOutsideUK): Renewal =
    this.copy(customersOutsideUK = Some(model), hasChanged = hasChanged || !this.customersOutsideUK.contains(model))

  def percentageOfCashPaymentOver15000(v: PercentageOfCashPaymentOver15000): Renewal =
    this.copy(percentageOfCashPaymentOver15000 = Some(v))

  def msbThroughput(model: MsbThroughput): Renewal =
    this.copy(msbThroughput = Some(model), hasChanged = hasChanged || !this.msbThroughput.contains(model))

  def msbWhichCurrencies(model: MsbWhichCurrencies): Renewal =
    this.copy(msbWhichCurrencies = Some(model), hasChanged = hasChanged || !this.msbWhichCurrencies.contains(model))
}

object Renewal {
  val key = "renewal"

  implicit val formats = Json.format[Renewal]

  implicit def default(renewal: Option[Renewal]): Renewal =
    renewal.getOrElse(Renewal())

}
