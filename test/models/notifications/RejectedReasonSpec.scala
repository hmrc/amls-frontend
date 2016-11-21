package models.notifications

import models.notifications.RejectedReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class RejectedReasonSpec extends PlaySpec {

  "RejectedReason model" must {
    "return reason for the string" in {
      RejectedReason.reason("01") must be(NonCompliant)
      RejectedReason.reason("02") must be(FailedToRespond)
      RejectedReason.reason("03") must be(FailedToPayCharges)
      RejectedReason.reason("04") must be(FitAndProperFailure)
      RejectedReason.reason("98") must be(OtherFailed)
      RejectedReason.reason("99") must be(OtherRefused)
    }

    "write data successfully" in {
      RejectedReason.jsonWrites.writes(NonCompliant) must be(JsString("01"))
      RejectedReason.jsonWrites.writes(FailedToRespond) must be(JsString("02"))
      RejectedReason.jsonWrites.writes(FailedToPayCharges) must be(JsString("03"))
      RejectedReason.jsonWrites.writes(FitAndProperFailure) must be(JsString("04"))
      RejectedReason.jsonWrites.writes(OtherFailed) must be(JsString("98"))
      RejectedReason.jsonWrites.writes(OtherRefused) must be(JsString("99"))
    }
  }
}
