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
          "portalId" -> "Default",
          "serviceName" -> "HMRC-MLR-ORG",
          "friendlyName" -> "AMLS Enrolment",
          "knownFacts" -> Seq(
            request.mlrRefNo,
            "",
            "",
            request.safeId
          )
        )
    }
}
