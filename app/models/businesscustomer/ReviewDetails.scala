package models.businesscustomer

import play.api.libs.json.Json

case class ReviewDetails(
                        businessName: String,
                        businessType: Option[String],
                        businessAddress: Address,
                        sapNumber: String,
                        safeId: String
//                        isAGroup: Boolean,
//                        directMatch: Boolean,
//                        agentReferenceNumber: Option[String],
//                        firstName: Option[String],
//                        lastName: Option[String]
                        )

object ReviewDetails {
  implicit val format = Json.format[ReviewDetails]
}
