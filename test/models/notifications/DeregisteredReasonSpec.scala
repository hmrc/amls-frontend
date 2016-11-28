package models.notifications

import models.notifications.DeregisteredReason._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsString

class DeregisteredReasonSpec extends PlaySpec {

  "DeregisteredReason model" must {
    "return reason for the string" in {
      DeregisteredReason.reason("01") must be(CeasedTrading)
      DeregisteredReason.reason("02") must be(HVDNoCashPayment)
      DeregisteredReason.reason("03") must be(OutOfScope)
      DeregisteredReason.reason("04") must be(NotTrading)
      DeregisteredReason.reason("05") must be(UnderAnotherSupervisor)
      DeregisteredReason.reason("06") must be(ChangeOfLegalEntity)
      DeregisteredReason.reason("99") must be(Other)
    }

    "write data successfully" in {
      DeregisteredReason.jsonWrites.writes(CeasedTrading) must be(JsString("01"))
      DeregisteredReason.jsonWrites.writes(HVDNoCashPayment) must be(JsString("02"))
      DeregisteredReason.jsonWrites.writes(OutOfScope) must be(JsString("03"))
      DeregisteredReason.jsonWrites.writes(NotTrading) must be(JsString("04"))
      DeregisteredReason.jsonWrites.writes(UnderAnotherSupervisor) must be(JsString("05"))
      DeregisteredReason.jsonWrites.writes(ChangeOfLegalEntity) must be(JsString("06"))
      DeregisteredReason.jsonWrites.writes(Other) must be(JsString("99"))
    }
  }
}
