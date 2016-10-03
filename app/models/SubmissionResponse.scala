package models

trait SubmissionResponse {
  val etmpFormBundleNumber: String
  val registrationFee: BigDecimal
  val fpFee: Option[BigDecimal]
  val premiseFee: BigDecimal
  val totalFees: BigDecimal
}