package models

trait SubmissionResponse {
  val etmpFormBundleNumber: String
  val registrationFee: BigDecimal
  val fpFee: Option[BigDecimal]
  val fpFeeRate: Option[BigDecimal]
  val premiseFee: BigDecimal
  val premiseFeeRate: Option[BigDecimal]
  val totalFees: BigDecimal
}
