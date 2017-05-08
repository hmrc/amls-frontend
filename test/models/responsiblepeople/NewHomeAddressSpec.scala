package models.responsiblepeople

import cats.data.Validated.Valid
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess}

class NewHomeAddressSpec extends PlaySpec {

  "NewHomeAddress" must {
    val viewMap = Map(
      "isUK" -> Seq("true"),
      "addressLine1" -> Seq("some address1"),
      "addressLine2" -> Seq("some address2"),
      "addressLine3" -> Seq("Default Line 3"),
      "addressLine4" -> Seq("Default Line 4"),
      "postCode" -> Seq("AA1 1AA")
    )

    val model = NewHomeAddress(PersonAddressUK("some address1","some address2",Some("Default Line 3"),
      Some("Default Line 4"),"AA1 1AA"))

    "read form data successfully when input is valid" in {
      NewHomeAddress.formRule.validate(viewMap) must be(Valid(NewHomeAddress(PersonAddressUK("some address1","some address2",Some("Default Line 3"),
        Some("Default Line 4"),"AA1 1AA"))))
    }

    "write form data successfully" in {
      NewHomeAddress.formWrites.writes(model) must be(viewMap)
    }

    "read/ write json successfully" in {
      NewHomeAddress.format.reads(NewHomeAddress.format.writes(model)) must be(JsSuccess(model, JsPath \ "personAddress" ))
    }

  }

}
