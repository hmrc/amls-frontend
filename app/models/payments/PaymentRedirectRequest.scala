package models.payments

import play.api.libs.json.Writes

case class PaymentRedirectRequest(reference: String, amount: Double, redirectUrl: ReturnLocation, internalId: Option[String] = None)

object PaymentRedirectRequest {

  implicit val jsonWriter: Writes[PaymentRedirectRequest] = {
    import play.api.libs.functional.syntax._
    import play.api.libs.json._
    (
      (__ \ "reference").write[String] and
        (__ \ "amount").write[String].contramap[Double](_.toString) and
        (__ \ "url").write[String].contramap[ReturnLocation](_.returnUrl) and
        (__ \ "internalId").writeNullable[String]
      ) (unlift(PaymentRedirectRequest.unapply))
  }

}
