package models

import models.estateagentbusiness.EstateAgentBusiness
import play.api.libs.json.Json

case class SubscriptionRequest(acknowledgmentReference: Option[String],
                               eabSection: Option[EstateAgentBusiness])

object SubscriptionRequest {
  implicit val format = Json.format[SubscriptionRequest]
}