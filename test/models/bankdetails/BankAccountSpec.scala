package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success

class BankAccountSpec extends PlaySpec with MockitoSugar {

  "Account" must {

    "validate form successfully for UKAccount" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      Account.formRule.validate(urlFormEncoded) must be(Success(UKAccount("12345678", "11-22-33")))
    }

    "validate form successfully for nonUKAccount" in {
      val urlFormEncoded = Map(
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "accountNumber" -> Seq("12345678")

      )

      Account.formRule.validate(urlFormEncoded) must be(Success(AccountNumber("12345678")))
    }
  }

}
