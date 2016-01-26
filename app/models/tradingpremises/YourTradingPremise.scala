package models.tradingpremises

import org.joda.time.LocalDate

case class YourTradingPremises(tradingName : String,
                              tradingAddress: TradingPremisesAddress,
                              startOfTradingDate: LocalDate,
                              isResidential : IsResidential)

sealed trait TradingPremisesAddress {

  def toLines: Seq[String] = this match {
    case a: TradingPremisesAddressUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.postCode)
      ).flatten
    case a: TradingPremisesAddressNonUK =>
      Seq(
        Some(a.addressLine1),
        Some(a.addressLine2),
        a.addressLine3,
        a.addressLine4,
        Some(a.country)
      ).flatten
  }
}

case class TradingPremisesAddressUK(
                               addressLine1: String,
                               addressLine2: String,
                               addressLine3: Option[String] = None,
                               addressLine4: Option[String] = None,
                               postCode: String
                             ) extends TradingPremisesAddress

case class TradingPremisesAddressNonUK(
                                  addressLine1: String,
                                  addressLine2: String,
                                  addressLine3: Option[String] = None,
                                  addressLine4: Option[String] = None,
                                  country: String
                                ) extends TradingPremisesAddress

sealed trait IsResidential
case object ResidentialYes extends IsResidential
case object ResidentialNo extends IsResidential
