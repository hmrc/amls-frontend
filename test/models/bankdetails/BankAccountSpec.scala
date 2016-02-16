package models.bankdetails

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.data.mapping.Success

class BankAccountSpec extends PlaySpec with MockitoSugar {

  "BankAccount" must {

    "validate form successfully for UKAccount" in {
      val model = Map (
        "accountName" -> Seq("test"),
        "isUK" -> Seq("true"),
        "accountNumber" -> Seq("12345678"),
        "sortCode" -> Seq("11-22-33")
      )

      BankAccount.formRule.validate(model) must be (Success(BankAccount("test", UKAccount("12345678", "11-22-33"))))
    }

    "validate form successfully for nonUKAccount" in {
      val model = Map (
        "accountName" -> Seq("test"),
        "isUK" -> Seq("false"),
        "accountNumber" -> Seq("12345678")

      )

      BankAccount.formRule.validate(model) must be (Success(BankAccount("test",AccountNumber("12345678"))))
    }
  }

}
