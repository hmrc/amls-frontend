package models.governmentgateway

import play.api.libs.json.{Writes, Json}

case class EnrolmentRequest(
                           mlrRefNo: String,
                           safeId: String
                           )

object EnrolmentRequest {

  implicit val writes: Writes[EnrolmentRequest] =
    Writes[EnrolmentRequest] {
      request =>
        Json.obj(
          "portalIdentifier" -> "Default",
          "serviceName" -> "HMRC-MLR-ORG",
          "friendlyName" -> "AMLS Enrolment",
          "knownFact" -> Seq(
            request.mlrRefNo,
            "",
            "",
            request.safeId
          )
        )
    }
}
