package models.notifications

import models.notifications.StatusType.{Approved, Revoked, Rejected, DeRegistered, Expired}
import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsString, JsSuccess}

class StatusSpec extends PlaySpec {

  "Status model" must {
    "must serialise and de serialise data successfully" in {
      val data = Status(Some(Approved),None)
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status Rejected" in {
      val data = Status(Some(Rejected),Some(RejectedReason.NonCompliant))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status Revoked" in {
      val data = Status(Some(Revoked), Some(RevokedReason.RevokedFitAndProperFailure))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status DeRegistered" in {
      val data = Status(Some(DeRegistered), Some(DeregisteredReason.HVDNoCashPayment))
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }


    "must serialise and de serialise data successfully for the status Expired" in {
      val data = Status(Some(Expired), None)
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "must serialise and de serialise data successfully for the status is None" in {
      val data = Status(None, None)
      Status.jsonReads.reads(Status.jsonWrites.writes(data)) must be(JsSuccess(data))
    }

    "fail with error when status value is passed incorrectly" in {
      StatusType.jsonReads.reads(JsString("12")) must be(JsError(List((JsPath ,List(ValidationError("error.invalid"))))))

    }
  }
}
