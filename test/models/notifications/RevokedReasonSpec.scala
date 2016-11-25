package models.notifications

import models.notifications.RevokedReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class RevokedReasonSpec extends PlaySpec {

  "RevokedReason model" must {
    "return reason for the string" in {
      RevokedReason.reason("01") must be(RevokedMissingTrader)
      RevokedReason.reason("02") must be(RevokedCeasedTrading)
      RevokedReason.reason("03") must be(RevokedNonCompliant)
      RevokedReason.reason("04") must be(RevokedFitAndProperFailure)
      RevokedReason.reason("05") must be(RevokedFailedToPayCharges)
      RevokedReason.reason("06") must be(RevokedFailedToRespond)
      RevokedReason.reason("99") must be(RevokedOther)
    }

    "write data successfully" in {
      RevokedReason.jsonWrites.writes(RevokedMissingTrader) must be(JsString("01"))
      RevokedReason.jsonWrites.writes(RevokedCeasedTrading) must be(JsString("02"))
      RevokedReason.jsonWrites.writes(RevokedNonCompliant) must be(JsString("03"))
      RevokedReason.jsonWrites.writes(RevokedFitAndProperFailure) must be(JsString("04"))
      RevokedReason.jsonWrites.writes(RevokedFailedToPayCharges) must be(JsString("05"))
      RevokedReason.jsonWrites.writes(RevokedFailedToRespond) must be(JsString("06"))
      RevokedReason.jsonWrites.writes(RevokedOther) must be(JsString("99"))
    }
  }
}
